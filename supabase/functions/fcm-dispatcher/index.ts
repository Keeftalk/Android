import { createClient } from 'npm:@supabase/supabase-js@2'
import { JWT } from 'npm:google-auth-library@9'

// ---------------------------------------------------------------------------
// Setup required BEFORE deploying this version:
//   1. supabase secrets set WEBHOOK_SECRET=<long random string>
//   2. On each Database Webhook (messages, call_sessions, message_reactions,
//      profile_views) add the HTTP header:  x-webhook-secret: <same string>
// Without both, the function answers 401 (that is the point: the anon key is
// public, so JWT verification alone does not stop forged pushes).
// ---------------------------------------------------------------------------

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
)

const WEBHOOK_SECRET = Deno.env.get('WEBHOOK_SECRET')
const FCM_MAX_BYTES = 3800 // FCM hard limit is 4096 bytes for the whole data payload

const s = (v: unknown): string => (v === null || v === undefined ? '' : String(v)) // FCM data values MUST be strings
const byteSize = (o: unknown) => new TextEncoder().encode(JSON.stringify(o)).length

// ---- Cached Google access token (valid ~1h) --------------------------------
let cachedToken: { value: string; expiresAt: number } | null = null

async function getAccessToken(): Promise<string> {
  if (cachedToken && cachedToken.expiresAt - Date.now() > 60_000) return cachedToken.value

  const clientEmail = Deno.env.get('FIREBASE_CLIENT_EMAIL')
  const privateKey = Deno.env.get('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n')
  if (!clientEmail || !privateKey) throw new Error('Missing Firebase credentials in environment')

  const jwt = new JWT({
    email: clientEmail,
    key: privateKey,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
  })
  const creds = await jwt.authorize()
  if (!creds.access_token) throw new Error('Google did not return an access token')

  cachedToken = {
    value: creds.access_token,
    expiresAt: creds.expiry_date ?? Date.now() + 50 * 60_000,
  }
  return cachedToken.value
}

type Recipient = { id: string; token: string }

async function addRecipient(list: Recipient[], userId: string) {
  const { data, error } = await supabase
    .from('profiles')
    .select('fcm_token')
    .eq('id', userId)
    .single()
  if (error) console.error(`profiles lookup failed for ${userId}:`, error.message)
  if (data?.fcm_token) list.push({ id: userId, token: data.fcm_token })
}

Deno.serve(async (req) => {
  if (!WEBHOOK_SECRET || req.headers.get('x-webhook-secret') !== WEBHOOK_SECRET) {
    return new Response('Unauthorized', { status: 401 })
  }

  try {
    const { table, type, record, old_record } = await req.json()

    const recipients: Recipient[] = []
    const fcmData: Record<string, string> = {}
    let senderId = ''
    let priority: 'HIGH' | 'NORMAL' = 'NORMAL'
    let ttl = '86400s'

    // --- CASE 1: NEW MESSAGES (Group & 1-to-1) -----------------------------
    if (table === 'messages' && type === 'INSERT') {
      senderId = record.sender_id
      priority = 'HIGH'

      const { data: members, error } = await supabase
        .from('chat_members')
        .select('user_id, profiles(fcm_token)')
        .eq('chat_id', record.chat_id)
        .neq('user_id', record.sender_id)
      // If this errors with PGRST200 there is no FK chat_members.user_id -> profiles.id
      if (error) console.error('chat_members query failed:', error)

      members?.forEach((m: any) => {
        const p = Array.isArray(m.profiles) ? m.profiles[0] : m.profiles
        if (p?.fcm_token) recipients.push({ id: m.user_id, token: p.fcm_token })
      })

      Object.assign(fcmData, {
        type: 'NEW_MESSAGE',
        chatId: s(record.chat_id),
        messageId: s(record.id),
        senderId: s(record.sender_id),
        body: s(record.content).slice(0, 300) || '[Encrypted]',
        ciphertext: s(record.ciphertext),
        nonce: s(record.nonce),
        cryptoVersion: s(record.crypto_version ?? 1),
        envelopeType: s(record.envelope_type ?? 100),
      })
    }
    // --- CASE 2: CALL SESSIONS (Start & End) -------------------------------
    else if (table === 'call_sessions') {
      priority = 'HIGH'
      ttl = '30s'

      if (type === 'INSERT' && record.state === 'calling') {
        senderId = record.caller_id
        await addRecipient(recipients, record.receiver_id)
        Object.assign(fcmData, {
          type: 'call',
          callId: s(record.id),
          chatId: s(record.chat_id),
          senderId: s(record.caller_id),
          callType: s(record.type || 'voice'),
        })
      } else if (type === 'UPDATE' && (record.state === 'ended' || record.state === 'rejected')) {
        // NOTE: routing logic unchanged from your version. Double-check intent:
        // a call rejected by the receiver (old state 'calling') pushes CANCEL_CALL
        // to the receiver, not to the caller.
        const notifyId = old_record?.state === 'calling' ? record.receiver_id : record.caller_id
        await addRecipient(recipients, notifyId)
        Object.assign(fcmData, { type: 'CANCEL_CALL', callId: s(record.id) })
      }
    }
    // --- CASE 3: MESSAGE REACTIONS ------------------------------------------
    else if (table === 'message_reactions' && type === 'INSERT') {
      senderId = record.user_id
      const { data: msg } = await supabase
        .from('messages')
        .select('sender_id, chat_id')
        .eq('id', record.message_id)
        .single()

      if (msg && msg.sender_id !== record.user_id) {
        await addRecipient(recipients, msg.sender_id)
        Object.assign(fcmData, {
          type: 'REACTION',
          chatId: s(msg.chat_id),
          messageId: s(record.message_id),
          emoji: s(record.emoji),
        })
      }
    }
    // --- CASE 4: PROFILE VIEWS ----------------------------------------------
    else if (table === 'profile_views' && type === 'INSERT') {
      senderId = record.viewer_id
      await addRecipient(recipients, record.viewed_id)
      Object.assign(fcmData, { type: 'profile_view', viewerId: s(record.viewer_id) })
    } else {
      return new Response('Event ignored', { status: 200 })
    }

    if (recipients.length === 0) return new Response('No recipients with valid tokens', { status: 200 })

    // --- Sender metadata (skipped for CANCEL_CALL: no sender, and '' is not a valid uuid) ---
    let senderName = 'Keeftalk'
    let senderAvatar = ''
    if (senderId) {
      const { data: sender, error } = await supabase
        .from('profiles')
        .select('full_name, username, avatar_url')
        .eq('id', senderId)
        .single()
      if (error) console.error('sender lookup failed:', error.message)
      senderName = sender?.full_name || sender?.username || 'Keeftalk'
      senderAvatar =
        sender?.avatar_url ||
        `https://ui-avatars.com/api/?name=${encodeURIComponent(sender?.username || 'U')}&background=random`
    }
    fcmData.title = senderName
    fcmData.avatar = senderAvatar

    if (fcmData.type === 'call') fcmData.callerName = senderName
    if (fcmData.type === 'REACTION') fcmData.body = `${senderName} reacted with ${fcmData.emoji}`
    if (fcmData.type === 'profile_view') fcmData.body = `${senderName} viewed your profile`

    // --- Payload size guard: drop ciphertext first (client falls back to DB sync), then trim body ---
    if (byteSize(fcmData) > FCM_MAX_BYTES) {
      fcmData.ciphertext = ''
      fcmData.nonce = ''
    }
    if (byteSize(fcmData) > FCM_MAX_BYTES && fcmData.body) {
      fcmData.body = fcmData.body.slice(0, 100)
    }

    // --- Dispatch ------------------------------------------------------------
    const projectId = Deno.env.get('FIREBASE_PROJECT_ID')
    if (!projectId) {
      console.error('Missing FIREBASE_PROJECT_ID')
      return new Response(JSON.stringify({ error: 'Missing Firebase credentials in environment' }), { status: 500 })
    }
    const accessToken = await getAccessToken()

    const results = await Promise.allSettled(
      recipients.map(async (r) => {
        const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({
            message: {
              token: r.token,
              data: fcmData,
              android: { priority, ttl },
            },
          }),
        })
        const json = await res.json().catch(() => ({}))

        if (!res.ok) {
          const code = json?.error?.details?.find((d: any) => d.errorCode)?.errorCode ?? json?.error?.status
          console.error(`FCM failed for user ${r.id}: HTTP ${res.status} ${code} - ${json?.error?.message}`)
          // Dead token: stop pushing to it until the app registers a fresh one
          if (code === 'UNREGISTERED') {
            await supabase.from('profiles').update({ fcm_token: null }).eq('id', r.id).eq('fcm_token', r.token)
          }
          return false
        }
        return true
      })
    )

    const sent = results.filter((x) => x.status === 'fulfilled' && x.value === true).length
    console.log(`FCM ${fcmData.type}: sent ${sent}/${recipients.length}`)
    return new Response(JSON.stringify({ success: sent > 0, sent, total: recipients.length }), { status: 200 })
  } catch (error) {
    console.error('Critical Function Error:', error)
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 })
  }
})