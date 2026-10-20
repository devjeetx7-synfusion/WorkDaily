-- WorkDaily Supabase Production Database Schema & RLS Policies
-- Firebae Auth UID is used as canonical user ID (auth.uid())

-- 1. Profiles Table
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    local_workspace_id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    username TEXT UNIQUE NOT NULL,
    email TEXT,
    photo_url TEXT,
    phone_number TEXT,
    country_code TEXT DEFAULT 'IN',
    currency_code TEXT DEFAULT 'INR',
    profession TEXT DEFAULT 'Site Worker',
    bio TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Follows Table
CREATE TABLE IF NOT EXISTS public.follows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_follow_edge UNIQUE (follower_id, following_id),
    CONSTRAINT prevent_self_follow CHECK (follower_id <> following_id)
);

-- 3. Friend Requests Table
CREATE TABLE IF NOT EXISTS public.friend_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT prevent_self_friend_request CHECK (sender_id <> receiver_id)
);

-- 4. Friendships Table
CREATE TABLE IF NOT EXISTS public.friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    user2_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_friendship UNIQUE (user1_id, user2_id),
    CONSTRAINT prevent_self_friendship CHECK (user1_id <> user2_id)
);

-- 5. Blocks Table
CREATE TABLE IF NOT EXISTS public.blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_block_edge UNIQUE (blocker_id, blocked_id),
    CONSTRAINT prevent_self_block CHECK (blocker_id <> blocked_id)
);

-- 6. Wallets Table
CREATE TABLE IF NOT EXISTS public.wallets (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    available_coins INT NOT NULL DEFAULT 0 CHECK (available_coins >= 0),
    pending_coins INT NOT NULL DEFAULT 0 CHECK (pending_coins >= 0),
    lifetime_earned_coins INT NOT NULL DEFAULT 0 CHECK (lifetime_earned_coins >= 0),
    diamond_balance INT NOT NULL DEFAULT 0 CHECK (diamond_balance >= 0),
    total_xp INT NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Wallet Transactions Table
CREATE TABLE IF NOT EXISTS public.wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    transaction_type TEXT NOT NULL CHECK (
        transaction_type IN (
            'CHECK_IN_REWARD',
            'SPIN_REWARD',
            'TASK_REWARD',
            'REFERRAL_REWARD',
            'REDEMPTION',
            'GIFT_SENT',
            'GIFT_RECEIVED',
            'CREATOR_EARNING',
            'WITHDRAWAL'
        )
    ),
    coin_delta INT NOT NULL DEFAULT 0,
    diamond_delta INT NOT NULL DEFAULT 0,
    description TEXT,
    idempotency_key TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. Reward Claims Table
CREATE TABLE IF NOT EXISTS public.reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    claim_type TEXT NOT NULL,
    claim_date DATE NOT NULL,
    coins_awarded INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_daily_claim UNIQUE (user_id, claim_type, claim_date)
);

-- 9. Referral Relationships Table
CREATE TABLE IF NOT EXISTS public.referral_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    referred_id UUID UNIQUE NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    referral_code TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT prevent_self_referral CHECK (referrer_id <> referred_id)
);

-- 10. Creator Earnings Table
CREATE TABLE IF NOT EXISTS public.creator_earnings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    total_diamonds_received INT NOT NULL DEFAULT 0 CHECK (total_diamonds_received >= 0),
    total_rupees_earned NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (total_rupees_earned >= 0),
    available_diamonds INT NOT NULL DEFAULT 0 CHECK (available_diamonds >= 0),
    pending_diamonds INT NOT NULL DEFAULT 0 CHECK (pending_diamonds >= 0),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. Gift Transactions Table
CREATE TABLE IF NOT EXISTS public.gift_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id TEXT NOT NULL,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    gift_id TEXT NOT NULL,
    gift_name TEXT NOT NULL,
    diamond_cost INT NOT NULL CHECK (diamond_cost > 0),
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 12. Withdrawal Requests Table
CREATE TABLE IF NOT EXISTS public.withdrawal_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    diamonds_converted INT NOT NULL CHECK (diamonds_converted > 0),
    amount_rupees NUMERIC(12, 2) NOT NULL CHECK (amount_rupees > 0),
    payout_method TEXT NOT NULL,
    account_identifier TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'REJECTED')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 13. Live Session Metadata Table
CREATE TABLE IF NOT EXISTS public.live_session_metadata (
    id TEXT PRIMARY KEY,
    host_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    room_type TEXT NOT NULL CHECK (room_type IN ('VIDEO', 'AUDIO', 'TEXT', 'YOUTUBE_WATCH_PARTY')),
    youtube_video_id TEXT,
    is_live BOOLEAN NOT NULL DEFAULT TRUE,
    peak_viewers INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ
);

-- ROW LEVEL SECURITY (RLS) POLICIES

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.wallet_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reward_claims ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.referral_relationships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.creator_earnings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gift_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.withdrawal_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.live_session_metadata ENABLE ROW LEVEL SECURITY;

-- Profiles: Public read, owner update
CREATE POLICY "Public profiles are viewable by authenticated users" ON public.profiles FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Wallets & Transactions: Owner read only
CREATE POLICY "Users can view their own wallet" ON public.wallets FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can view their own transactions" ON public.wallet_transactions FOR SELECT USING (auth.uid() = user_id);

-- Creator Earnings: Owner read only
CREATE POLICY "Creators can view their own earnings" ON public.creator_earnings FOR SELECT USING (auth.uid() = user_id);

-- Follows: Authenticated users view and manage own follow edges
CREATE POLICY "View follows" ON public.follows FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Insert follow" ON public.follows FOR INSERT WITH CHECK (auth.uid() = follower_id);
CREATE POLICY "Delete follow" ON public.follows FOR DELETE USING (auth.uid() = follower_id);

-- Live Session Metadata: Viewable by all authenticated users, updateable by host
CREATE POLICY "View live streams" ON public.live_session_metadata FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Manage live stream" ON public.live_session_metadata FOR ALL USING (auth.uid() = host_id);
