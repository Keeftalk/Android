import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { JWT } from 'https://esm.sh/google-auth-library@9'

Deno.serve(async (req) => {
  try {
    const payload = await req.json()
    const { table, type, record, old_record } = payload

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const recipients = [] // Array of { id, token }
    let senderId = ''
    let fcmData = {}

    // --- CASE 1: NEW MESSAGES (Group & 1-to-1) ---
    if (table === 'messages' && type === 'INSERT') {
      senderId = record.sender_id
      const { data: members } = await supabase
        .from('chat_members')
        .select('user_id, profiles(fcm_token)')
        .eq('chat_id', record.chat_id)
        .neq('user_id', record.sender_id)

      members?.forEach(m => {
        if (m.profiles?.fcm_token) recipients.push({ id: m.user_id, token: m.profiles.fcm_token })
      })

      fcmData = {
        type: "NEW_MESSAGE",
        chatId: record.chat_id,
        messageId: record.id,
        senderId: record.sender_id,
        body: record.content, // Fallback body
        ciphertext: record.ciphertext || "",
        nonce: record.nonce || "",
        cryptoVersion: (record.crypto_version || 1).toString(),
        envelopeType: (record.envelope_type || 100).toString()
      }
    }
    // --- CASE 2: CALL SESSIONS (Start & End) ---
    else if (table === 'call_sessions') {
      if (type === 'INSERT' && record.state === 'calling') {
        senderId = record.caller_id
        const { data: profile } = await supabase.from('profiles').select('fcm_token').eq('id', record.receiver_id).single()
        if (profile?.fcm_token) recipients.push({ id: record.receiver_id, token: profile.fcm_token })

        fcmData = {
          type: "call",
          callId: record.id,
          chatId: record.chat_id,
          senderId: record.caller_id,
          callType: record.type || "voice"
        }
      } else if (type === 'UPDATE' && (record.state === 'ended' || record.state === 'rejected')) {
        // Notify the receiver to stop ringing if the caller hung up, or vice-versa
        const notifyId = (old_record?.state === 'calling') ? record.receiver_id : record.caller_id
        const { data: profile } = await supabase.from('profiles').select('fcm_token').eq('id', notifyId).single()
        if (profile?.fcm_token) recipients.push({ id: notifyId, token: profile.fcm_token })

        fcmData = { type: "CANCEL_CALL", callId: record.id }
      }
    }
    // --- CASE 3: MESSAGE REACTIONS ---
    else if (table === 'message_reactions' && type === 'INSERT') {
      senderId = record.user_id
      const { data: msg } = await supabase.from('messages').select('sender_id, chat_id').eq('id', record.message_id).single()
      if (msg && msg.sender_id !== record.user_id) {
        const { data: profile } = await supabase.from('profiles').select('fcm_token').eq('id', msg.sender_id).single()
        if (profile?.fcm_token) recipients.push({ id: msg.sender_id, token: profile.fcm_token })

        fcmData = {
          type: "REACTION",
          chatId: msg.chat_id,
          messageId: record.message_id,
          emoji: record.emoji
        }
      }
    }
    // --- CASE 4: PROFILE VIEWS ---
    else if (table === 'profile_views' && type === 'INSERT') {
      senderId = record.viewer_id
      const { data: profile } = await supabase.from('profiles').select('fcm_token').eq('id', record.viewed_id).single()
      if (profile?.fcm_token) recipients.push({ id: record.viewed_id, token: profile.fcm_token })

      fcmData = { type: "profile_view", viewerId: record.viewer_id }
    }
    else {
      return new Response('Event ignored', { status: 200 })
    }

    if (recipients.length === 0) return new Response('No recipients with valid tokens', { status: 200 })

    // 2. Fetch Sender Profile for UI Metadata
    const { data: sender } = await supabase
      .from('profiles')
      .select('full_name, username, avatar_url')
      .eq('id', senderId)
      .single()

    fcmData.title = sender?.full_name || sender?.username || "Keeftalk"
    fcmData.avatar = sender?.avatar_url || `https://ui-avatars.com/api/?name=${sender?.username || 'U'}&background=random`

    // Customize body based on type
    if (fcmData.type === "call") fcmData.callerName = fcmData.title
    if (fcmData.type === "REACTION") fcmData.body = `${fcmData.title} reacted with ${fcmData.emoji}`
    if (fcmData.type === "profile_view") fcmData.body = `${fcmData.title} viewed your profile`

    // 3. Authenticate with Firebase via Service Account
    const jwt = new JWT(
      Deno.env.get('FIREBASE_CLIENT_EMAIL'),
      undefined,
      Deno.env.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n'),
      ['https://www.googleapis.com/auth/firebase.messaging']
    )
    const tokens = await jwt.authorize()

    // 4. Dispatch FCMs in Parallel
    const results = await Promise.all(recipients.map(r =>
      fetch(`https://fcm.googleapis.com/v1/projects/${Deno.env.get('FIREBASE_PROJECT_ID')}/messages:send`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${tokens.access_token}`
        },
        body: JSON.stringify({
          message: {
            token: r.token,
            data: fcmData,
            android: {
              priority: (fcmData.type === "call" || fcmData.type === "CANCEL_CALL") ? "HIGH" : "NORMAL",
              ttl: fcmData.type === "call" ? "30s" : "3600s"
            }
          }
        })
      }).then(res => res.json())
    ))

    console.log("Batch FCM Results:", results)
    return new Response(JSON.stringify({ success: true, count: results.length }), { status: 200 })

  } catch (error) {
    console.error("Critical Function Error:", error)
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }
})
