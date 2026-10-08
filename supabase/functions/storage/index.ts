import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { AwsClient } from 'https://esm.sh/aws4fetch@1.0.17'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

const R2_ACCESS_KEY_ID = Deno.env.get('R2_ACCESS_KEY_ID') || 'mock_access_key'
const R2_SECRET_ACCESS_KEY = Deno.env.get('R2_SECRET_ACCESS_KEY') || 'mock_secret_key'
const R2_ENDPOINT = Deno.env.get('R2_ENDPOINT') || 'https://5cfb75b2169676dc91bd6a57b7d6d7ab.r2.cloudflarestorage.com'

const PROVISIONED_PRIVATE_BUCKETS = new Set([
  'keeftalk-weur-private',
  'keeftalk-eeur-private',
  'keeftalk-enam-private',
  'keeftalk-apac-private',
  'keeftalk-oc-private'
])

const PROVISIONED_PUBLIC_BUCKETS = new Set([
  'keeftalk-weur-public',
  'keeftalk-eeur-public',
  'keeftalk-enam-public',
  'keeftalk-apac-public',
  'keeftalk-oc-public'
])

const r2 = new AwsClient({
  accessKeyId: R2_ACCESS_KEY_ID,
  secretAccessKey: R2_SECRET_ACCESS_KEY,
  service: 's3',
  region: 'auto',
})

// Helper to generate a full endpoint path URL
export const getR2Url = (bkt: string, pth: string) => {
  const urlObj = new URL(R2_ENDPOINT)
  const url = new URL(urlObj.origin)
  if (!url.pathname.endsWith('/')) url.pathname += '/'
  url.pathname += `${bkt}/${pth.startsWith('/') ? pth.slice(1) : pth}`
  return url
}

export function validateCategory(category?: string): string {
  const cleanCat = (category || 'files').toLowerCase().trim()
  if (['files', 'archives', 'exports', 'taging'].includes(cleanCat)) {
    return cleanCat
  }
  return 'files'
}

export function buildVaultObjectKey(countryCode: string, userId: string, category: string, fileId: string, subpath: string = 'original'): string {
  const cleanCountry = countryCode.toUpperCase().trim()
  const cleanCat = validateCategory(category)
  const cleanSub = subpath.trim().replace(/^\/+|\/+$/g, '')
  return `${cleanCountry}/${userId}/${cleanCat}/${fileId}/${cleanSub}`
}

export function isBucketProvisioned(bucket: string): boolean {
  if (bucket.includes('public')) {
    return PROVISIONED_PUBLIC_BUCKETS.has(bucket)
  }
  return PROVISIONED_PRIVATE_BUCKETS.has(bucket)
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
    if (!supabaseUrl || !supabaseKey) {
      return new Response(JSON.stringify({ error: 'Server configuration error' }), { status: 500, headers: corsHeaders })
    }

    const supabase = createClient(supabaseUrl, supabaseKey)

    const authHeader = req.headers.get('Authorization')
    const token = authHeader?.split(' ')[1]
    if (!token) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Missing token' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid token' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 1. Resolve user's assigned storage region and buckets server-side
    let { data: storageAssignment } = await supabase
      .from('user_storage_regions')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle()

    if (!storageAssignment) {
      const defaultRegion = 'WEUR'
      const defaultCountry = 'TN'
      const { data: assignedRows } = await supabase.rpc('get_or_assign_user_storage_region', {
        p_user_id: user.id,
        p_country_code: defaultCountry,
        p_storage_region: defaultRegion,
        p_private_bucket: 'keeftalk-weur-private',
        p_public_bucket: 'keeftalk-weur-public',
        p_market_bucket: 'keeftalk-weur-market',
        p_detention_bucket: 'keeftalk-weur-detention',
        p_backups_bucket: 'keeftalk-weur-backups'
      })
      if (assignedRows && assignedRows.length > 0) {
        storageAssignment = assignedRows[0]
      } else {
        storageAssignment = {
          country_code: 'TN',
          storage_region: 'WEUR',
          private_bucket: 'keeftalk-weur-private',
          public_bucket: 'keeftalk-weur-public',
          market_bucket: 'keeftalk-weur-market',
          detention_bucket: 'keeftalk-weur-detention',
          backups_bucket: 'keeftalk-weur-backups'
        }
      }
    }

    const countryCode = storageAssignment.country_code.toUpperCase()
    const targetBucket = storageAssignment.private_bucket

    // 2. Validate that the target regional bucket is provisioned ahead of time
    if (!isBucketProvisioned(targetBucket)) {
      console.error(`[INFRASTRUCTURE_ERROR] Missing regional R2 bucket: ${targetBucket} for user ${user.id}`)
      return new Response(JSON.stringify({
        error: `INFRASTRUCTURE_ERROR: Regional R2 storage bucket '${targetBucket}' is not provisioned`,
        code: 'BUCKET_NOT_FOUND',
        bucket: targetBucket
      }), { status: 503, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    const body = await req.json().catch(() => ({}))
    const { action, fileId, category, subpath, path, contentType, chatId } = body

    // Server-side object key construction
    let objectKey = path
    if (!objectKey && fileId) {
      objectKey = buildVaultObjectKey(countryCode, user.id, category || 'files', fileId, subpath || 'original')
    }

    // Authorize path ownership if path supplied by client
    if (path) {
      const allowedUserPrefix = `${countryCode}/${user.id}/`
      const legacyUserPrefix = `${user.id}/`
      const isUserPath = path.startsWith(allowedUserPrefix) || path.startsWith(legacyUserPrefix)

      let isChatAuthorized = false
      if (chatId) {
        const { data: member } = await supabase
          .from('chat_members')
          .select('chat_id')
          .eq('chat_id', chatId)
          .eq('user_id', user.id)
          .maybeSingle()
        if (member) isChatAuthorized = true
      }

      if (!isUserPath && !isChatAuthorized) {
        console.warn(`[UNAUTHORIZED_PATH] User ${user.id} attempted to access unauthorized path: ${path}`)
        return new Response(JSON.stringify({ error: 'Forbidden: Cannot access paths belonging to another user' }), { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }
    }

    // ACTION: get-upload-url
    if (action === 'get-upload-url') {
      if (!fileId) {
        return new Response(JSON.stringify({ error: 'Missing fileId' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const finalKey = buildVaultObjectKey(countryCode, user.id, category || 'files', fileId, subpath || 'original')
      const targetUrl = getR2Url(targetBucket, finalKey)

      console.log(`Presigning UPLOAD | User: ${user.id} | Bucket: ${targetBucket} | Key: ${finalKey}`)

      const signedRequest = await r2.sign(targetUrl.toString(), {
        method: 'PUT',
        aws: { signQuery: true }
      })

      return new Response(JSON.stringify({
        url: signedRequest.url,
        objectKey: finalKey,
        bucket: targetBucket,
        countryCode: countryCode,
        region: storageAssignment.storage_region
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // ACTION: get-download-url
    if (action === 'get-download-url') {
      const finalKey = objectKey || (fileId ? buildVaultObjectKey(countryCode, user.id, category || 'files', fileId, subpath || 'original') : null)
      if (!finalKey) {
        return new Response(JSON.stringify({ error: 'Missing fileId or path' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const targetUrl = getR2Url(targetBucket, finalKey)
      console.log(`Presigning DOWNLOAD | User: ${user.id} | Bucket: ${targetBucket} | Key: ${finalKey}`)

      const signedRequest = await r2.sign(targetUrl.toString(), {
        method: 'GET',
        aws: { signQuery: true }
      })

      return new Response(JSON.stringify({
        url: signedRequest.url,
        objectKey: finalKey
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // ACTION: finalize-upload
    if (action === 'finalize-upload') {
      if (!objectKey) {
        return new Response(JSON.stringify({ error: 'Missing objectKey' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const targetUrl = getR2Url(targetBucket, objectKey)
      const headRes = await r2.fetch(targetUrl.toString(), { method: 'HEAD' }).catch(() => null)

      const size = headRes && headRes.ok ? parseInt(headRes.headers.get('content-length') || '0', 10) : 0

      if (fileId) {
        await supabase.from('files').update({
          status: 'ACTIVE',
          physical_size: size,
          storage_path: objectKey,
          updated_at: new Date().toISOString()
        }).eq('id', fileId).eq('owner_id', user.id)
      }

      return new Response(JSON.stringify({ success: true, size, objectKey }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // ACTION: move-to-trash
    if (action === 'move-to-trash') {
      if (!fileId) {
        return new Response(JSON.stringify({ error: 'Missing fileId' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const sourceKey = objectKey || buildVaultObjectKey(countryCode, user.id, 'files', fileId, 'original')
      const destKey = buildVaultObjectKey(countryCode, user.id, 'archives', fileId, 'original')

      const sourceUrl = getR2Url(targetBucket, sourceKey)
      const destUrl = getR2Url(targetBucket, destKey)

      const copyRes = await r2.fetch(destUrl.toString(), {
        method: 'PUT',
        headers: { 'x-amz-copy-source': `/${targetBucket}/${sourceKey}` }
      }).catch(() => null)

      if (copyRes && copyRes.ok) {
        await r2.fetch(sourceUrl.toString(), { method: 'DELETE' }).catch(() => null)
      }

      await supabase.from('files').update({
        status: 'TRASH',
        archived_at: new Date().toISOString(),
        scheduled_purge_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        storage_path: destKey,
        original_path: sourceKey
      }).eq('id', fileId).eq('owner_id', user.id)

      return new Response(JSON.stringify({ success: true, newPath: destKey }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // ACTION: restore-from-trash
    if (action === 'restore-from-trash') {
      if (!fileId) {
        return new Response(JSON.stringify({ error: 'Missing fileId' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const sourceKey = objectKey || buildVaultObjectKey(countryCode, user.id, 'archives', fileId, 'original')
      const destKey = buildVaultObjectKey(countryCode, user.id, 'files', fileId, 'original')

      const sourceUrl = getR2Url(targetBucket, sourceKey)
      const destUrl = getR2Url(targetBucket, destKey)

      const copyRes = await r2.fetch(destUrl.toString(), {
        method: 'PUT',
        headers: { 'x-amz-copy-source': `/${targetBucket}/${sourceKey}` }
      }).catch(() => null)

      if (copyRes && copyRes.ok) {
        await r2.fetch(sourceUrl.toString(), { method: 'DELETE' }).catch(() => null)
      }

      await supabase.from('files').update({
        status: 'ACTIVE',
        archived_at: null,
        scheduled_purge_at: null,
        storage_path: destKey
      }).eq('id', fileId).eq('owner_id', user.id)

      return new Response(JSON.stringify({ success: true, newPath: destKey }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // ACTION: delete-object / permanent-purge
    if (action === 'delete-object' || action === 'permanent-purge') {
      if (!objectKey && !fileId) {
        return new Response(JSON.stringify({ error: 'Missing fileId or path' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      const targetKey = objectKey || buildVaultObjectKey(countryCode, user.id, 'files', fileId!, 'original')
      const targetUrl = getR2Url(targetBucket, targetKey)

      await r2.fetch(targetUrl.toString(), { method: 'DELETE' }).catch(() => null)

      if (fileId && action === 'permanent-purge') {
        await supabase.from('files').delete().eq('id', fileId).eq('owner_id', user.id)
      }

      return new Response(JSON.stringify({ success: true }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    return new Response(JSON.stringify({ error: 'Invalid action' }), { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })

  } catch (err: any) {
    console.error('Storage Edge Function Error:', err)
    return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
