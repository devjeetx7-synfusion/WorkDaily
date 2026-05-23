import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

// TODO: Replace with your Firebase project config
const firebaseConfig = {
  apiKey: "AIzaSyCN4mRgnpqR5oVh3o3HF0DjiADgllW3_mA",
  authDomain: "crovian-2ae30.firebaseapp.com",
  databaseURL: "https://crovian-2ae30-default-rtdb.firebaseio.com",
  projectId: "crovian-2ae30",
  storageBucket: "crovian-2ae30.firebasestorage.app",
  messagingSenderId: "811601656329",
  appId: "1:811601656329:android:d8acba5ba4ce12c7a616d8",
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const googleProvider = new GoogleAuthProvider();
