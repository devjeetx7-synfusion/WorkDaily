'use strict';

import { AccessToken, TrackSource } from 'livekit-server-sdk';
import { createRemoteJWKSet, jwtVerify } from 'jose';

const TOKEN_TTL_SECONDS = 300;
const ROOM_ID_REGEX = /^[A-Za-z0-9_-]{1,128}$/;

// Google's public certificates for Firebase Auth ID token verification
const FIREBASE_JWKS = createRemoteJWKSet(
  new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com')
);

/**
 * Validates a Firebase ID Token and returns the verified decoded payload.
 */
export async function verifyFirebaseIdToken(idToken, projectId) {
  if (!idToken || typeof idToken !== 'string') {
    throw new Error('Missing authentication token.');
  }
  if (!projectId || typeof projectId !== 'string') {
    throw new Error('Server configuration error: missing Firebase Project ID.');
  }

  const { payload } = await jwtVerify(idToken, FIREBASE_JWKS, {
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId,
  });

  const uid = payload.sub || payload.user_id;
  if (!uid || typeof uid !== 'string') {
    throw new Error('Invalid token subject.');
  }
  return { uid, payload };
}

/**
 * Converts a raw Firestore REST API document object to a simple JS object.
 */
export function firestoreFieldsToData(fields) {
  if (!fields) return null;
  const result = {};
  for (const [key, valueObj] of Object.entries(fields)) {
    if ('stringValue' in valueObj) result[key] = valueObj.stringValue;
    else if ('booleanValue' in valueObj) result[key] = valueObj.booleanValue;
    else if ('integerValue' in valueObj) result[key] = parseInt(valueObj.integerValue, 10);
    else if ('doubleValue' in valueObj) result[key] = parseFloat(valueObj.doubleValue);
    else if ('nullValue' in valueObj) result[key] = null;
    else if ('arrayValue' in valueObj) {
      result[key] = (valueObj.arrayValue.values || []).map(v => {
        if ('stringValue' in v) return v.stringValue;
        return v;
      });
    }
  }
  return result;
}

/**
 * Fetches a Firestore document using Firestore REST API with the user's ID token.
 */
export async function getFirestoreDoc(projectId, collectionPath, docId, idToken) {
  const url = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${collectionPath}/${encodeURIComponent(docId)}`;
  const resp = await fetch(url, {
    headers: {
      Authorization: `Bearer ${idToken}`,
    },
  });
  if (resp.status === 404) return null;
  if (!resp.ok) {
    const errorText = await resp.text();
    throw new Error(`Firestore query failed (${resp.status}): ${errorText}`);
  }
  const json = await resp.json();
  return firestoreFieldsToData(json.fields);
}

/**
 * Checks whether userA has blocked userB or vice-versa.
 */
export async function isBlocked(projectId, userA, userB, idToken) {
  try {
    const doc1 = await getFirestoreDoc(projectId, 'blocks', `${userA}_${userB}`, idToken);
    if (doc1) return true;
    const doc2 = await getFirestoreDoc(projectId, 'blocks', `${userB}_${userA}`, idToken);
    return !!doc2;
  } catch (_) {
    // If query fails or blocked, fail closed
    return false;
  }
}

/**
 * Core token handler logic. Can be invoked with mocks for testing.
 */
export async function handleTokenRequest({
  uid,
  roomId,
  idToken,
  env,
  fetchRoom = (pid, rId, token) => getFirestoreDoc(pid, 'live_rooms', rId, token),
  fetchMember = (pid, rId, mUid, token) => getFirestoreDoc(pid, `live_rooms/${rId}/members`, mUid, token),
  fetchLock = (pid, hUid, token) => getFirestoreDoc(pid, 'live_media_host_locks', hUid, token),
  checkBlocked = (pid, u1, u2, token) => isBlocked(pid, u1, u2, token),
  clock = Date.now,
}) {
  if (!roomId || typeof roomId !== 'string' || !ROOM_ID_REGEX.test(roomId)) {
    return { status: 400, body: { error: 'Invalid room ID.' } };
  }

  const { LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET, FIREBASE_PROJECT_ID } = env;

  let parsedUrl;
  try {
    parsedUrl = new URL(LIVEKIT_URL);
  } catch (_) {}
  if (!parsedUrl || parsedUrl.protocol !== 'wss:' || parsedUrl.username || parsedUrl.password ||
      !LIVEKIT_API_KEY || !LIVEKIT_API_SECRET || !FIREBASE_PROJECT_ID) {
    return { status: 500, body: { error: 'Live streaming server is not properly configured.' } };
  }

  // 1. Fetch Room from Firestore
  let room;
  try {
    room = await fetchRoom(FIREBASE_PROJECT_ID, roomId, idToken);
  } catch (err) {
    return { status: 500, body: { error: 'Failed to retrieve room details.' } };
  }

  if (!room || room.mediaManaged !== true || !['VIDEO', 'AUDIO'].includes(room.type) ||
      room.endedAtMillis != null || typeof room.hostUserId !== 'string' || !room.hostUserId) {
    return { status: 404, body: { error: 'This live room is unavailable or has ended.' } };
  }

  const isHost = room.hostUserId === uid;
  const now = clock();

  // 2. Validate Room State & Host Reservation
  const preparing = isHost && room.mediaManaged === true && room.isLive === false &&
    room.statusText === 'PREPARING' && Number.isSafeInteger(room.reservationExpiresAtMillis) &&
    room.reservationExpiresAtMillis > now;

  let hasValidLock = false;
  if (preparing) {
    try {
      const lock = await fetchLock(FIREBASE_PROJECT_ID, uid, idToken);
      hasValidLock = lock && lock.roomId === roomId;
    } catch (_) {
      hasValidLock = false;
    }
  }

  const isPreparingAuthorized = preparing && hasValidLock;

  if (!(room.isLive === true && room.statusText === 'LIVE') && !isPreparingAuthorized) {
    return { status: 400, body: { error: 'This live room is unavailable or has ended.' } };
  }

  // 3. For Viewers: verify room is public, not blocked, and membership exists
  if (!isHost) {
    if (room.isPublic !== true) {
      return { status: 403, body: { error: 'You cannot join this private live room.' } };
    }
    const blocked = await checkBlocked(FIREBASE_PROJECT_ID, uid, room.hostUserId, idToken);
    if (blocked) {
      return { status: 403, body: { error: 'You cannot join this live room.' } };
    }
    const member = await fetchMember(FIREBASE_PROJECT_ID, roomId, uid, idToken);
    if (!member || member.uid !== uid) {
      return { status: 403, body: { error: 'Membership required before joining stream.' } };
    }
  }

  // 4. Generate LiveKit Access Token
  const roomName = `workdaily-${roomId}`;
  const at = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
    identity: uid,
    ttl: TOKEN_TTL_SECONDS,
  });

  at.addGrant({
    roomJoin: true,
    room: roomName,
    canPublish: isHost,
    canSubscribe: true,
    canPublishData: false,
    canUpdateOwnMetadata: false,
    ...(isHost
      ? {
          canPublishSources:
            room.type === 'VIDEO'
              ? [TrackSource.CAMERA, TrackSource.MICROPHONE]
              : [TrackSource.MICROPHONE],
        }
      : {}),
  });

  const jwt = await at.toJwt();

  return {
    status: 200,
    body: {
      url: LIVEKIT_URL,
      token: jwt,
      roomName,
      expiresInSeconds: TOKEN_TTL_SECONDS,
    },
  };
}

export default {
  async fetch(request, env) {
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    if (request.method !== 'POST' || (url.pathname !== '/liveKitToken' && url.pathname !== '/')) {
      return new Response(JSON.stringify({ error: 'Not found' }), {
        status: 404,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // Extract Bearer token
    const authHeader = request.headers.get('Authorization') || '';
    if (!authHeader.startsWith('Bearer ')) {
      return new Response(JSON.stringify({ error: 'Missing or invalid Authorization header.' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    const idToken = authHeader.slice(7).trim();
    if (!idToken) {
      return new Response(JSON.stringify({ error: 'Missing ID token.' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // Verify Firebase ID Token
    let uid;
    try {
      const verified = await verifyFirebaseIdToken(idToken, env.FIREBASE_PROJECT_ID);
      uid = verified.uid;
    } catch (err) {
      return new Response(JSON.stringify({ error: 'Invalid or expired authentication token.' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // Parse body
    let body;
    try {
      body = await request.json();
    } catch (_) {
      return new Response(JSON.stringify({ error: 'Invalid JSON request body.' }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    const roomId = body?.roomId;
    try {
      const result = await handleTokenRequest({
        uid,
        roomId,
        idToken,
        env,
      });

      return new Response(JSON.stringify(result.body), {
        status: result.status,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    } catch (err) {
      return new Response(JSON.stringify({ error: 'Internal server error.' }), {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }
  },
};
