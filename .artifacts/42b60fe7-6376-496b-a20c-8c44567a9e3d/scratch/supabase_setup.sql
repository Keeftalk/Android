-- Calendar Items Table
CREATE TABLE IF NOT EXISTS calendar_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- event, task, reminder, birthday, meeting, goal
    title TEXT NOT NULL,
    description TEXT,
    color TEXT,
    icon TEXT,
    location TEXT,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    is_all_day BOOLEAN DEFAULT false,
    timezone TEXT,
    recurrence_rule TEXT,
    priority TEXT, -- Low, Medium, High
    status TEXT, -- Pending, Completed, Cancelled
    is_private BOOLEAN DEFAULT false,
    category_id UUID,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    meeting_type TEXT,
    meeting_link TEXT,
    progress INTEGER DEFAULT 0,
    pomodoro_count INTEGER DEFAULT 0,
    deadline TIMESTAMPTZ,
    parent_item_id UUID
);

-- Family Members Table (Linking users together)
CREATE TABLE IF NOT EXISTS family_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    member_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    included BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, member_id)
);

-- Family Permissions Table
CREATE TABLE IF NOT EXISTS family_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    family_member_id UUID REFERENCES family_members(id) ON DELETE CASCADE,
    type TEXT NOT NULL, -- tasks, birthdays, events, reminders, meetings, goals
    is_allowed BOOLEAN DEFAULT true,
    UNIQUE(family_member_id, type)
);

-- Enable RLS
ALTER TABLE calendar_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_permissions ENABLE ROW LEVEL SECURITY;

-- Policies for calendar_items
CREATE POLICY "Users can view their own items" ON calendar_items
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can view shared family items" ON calendar_items
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM family_members fm
            JOIN family_permissions fp ON fm.id = fp.family_member_id
            WHERE fm.member_id = calendar_items.user_id
            AND fm.user_id = auth.uid()
            AND fm.included = true
            AND fp.type = calendar_items.type
            AND fp.is_allowed = true
        )
    );

CREATE POLICY "Users can manage their own items" ON calendar_items
    FOR ALL USING (auth.uid() = user_id);

-- Policies for family_members
CREATE POLICY "Users can view their family links" ON family_members
    FOR SELECT USING (auth.uid() = user_id OR auth.uid() = member_id);

CREATE POLICY "Users can manage their family links" ON family_members
    FOR ALL USING (auth.uid() = user_id);

-- Policies for family_permissions
CREATE POLICY "Users can view their family permissions" ON family_permissions
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM family_members fm
            WHERE fm.id = family_permissions.family_member_id
            AND (fm.user_id = auth.uid() OR fm.member_id = auth.uid())
        )
    );

CREATE POLICY "Users can manage their family permissions" ON family_permissions
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM family_members fm
            WHERE fm.id = family_permissions.family_member_id
            AND fm.user_id = auth.uid()
        )
    );
