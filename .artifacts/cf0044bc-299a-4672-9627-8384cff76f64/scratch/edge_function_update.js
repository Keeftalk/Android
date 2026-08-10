import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { JWT } from 'https://esm.sh/google-auth-library@9'

Deno.serve(async (req) => {
  try {
    const payload = await req.json()
    const message = payload.record

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // 1. Get Recipient and Sender Info
    const { data: members } = await supabase
      .from('chat_members')
      .select('user_id')
      .eq('chat_id', message.chat_id)
      .neq('user_id', message.sender_id)
      .single()

    if (!members) return new Response('No recipient found', { status: 200 })

    const { data: sender } = await supabase
      .from('profiles')
      .select('full_name, username, avatar_url')
      .eq('id', message.sender_id)
      .single()

    const { data: recipient } = await supabase
      .from('profiles')
      .select('fcm_token')
      .eq('id', members.user_id)
      .single()

    if (!recipient?.fcm_token) return new Response('No FCM token found', { status: 200 })

    // 2. Auth with Firebase
    const jwt = new JWT(
      Deno.env.get('FIREBASE_CLIENT_EMAIL'),
      undefined,
      Deno.env.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n'),
      ['https://www.googleapis.com/auth/firebase.messaging']
    )
    const tokens = await jwt.authorize()

    // 3. Send FCM (Data-only payload to show avatar instead of app logo)
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${Deno.env.get('FIREBASE_PROJECT_ID')}/messages:send`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${tokens.access_token}`
        },
        body: JSON.stringify({
          message: {
            token: recipient.fcm_token,
            data: {
              title: sender?.full_name || sender?.username || "New Message",
              body: message.content, // Usually [Encrypted]
              avatar: sender?.avatar_url || `https://ui-avatars.com/api/?name=${sender?.username || 'U'}&background=random`,
              chatId: message.chat_id,
              senderId: message.sender_id,
              messageId: message.id, // REQ: For database wait fallback
              ciphertext: message.ciphertext || "", // REQ: For Instant Decryption
              nonce: message.nonce || "", // REQ: For Instant Decryption
              cryptoVersion: (message.crypto_version || 1).toString(), // REQ: For Instant Decryption
              envelopeType: (message.envelope_type || 100).toString(), // REQ: For Instant Decryption
              type: "NEW_MESSAGE"
            },
            android: {
              priority: "HIGH" // Required to wake up the device for data-only messages
            }
          }
        })
      }
    )

    const result = await res.text()
    console.log("FCM Response:", result)
    return new Response(result, { status: 200 })

  } catch (error) {
    console.error("Function Error:", error)
    return new Response(JSON.stringify({ error: error.message }), { status: 500 })
  }
})
