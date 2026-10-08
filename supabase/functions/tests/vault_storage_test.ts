import { assertEquals, assertFalse, assertStringIncludes } from 'https://deno.land/std@0.208.0/assert/mod.ts'
import { AwsClient } from 'https://esm.sh/aws4fetch@1.0.17'
import { buildVaultObjectKey, isBucketProvisioned, getR2Url, validateCategory } from '../storage/index.ts'

Deno.test('1. Regional Bucket Mapping for All Supported Regions', () => {
  const regions: Record<string, { country: string; expectedBucket: string }> = {
    WEUR: { country: 'TN', expectedBucket: 'keeftalk-weur-private' },
    EEUR: { country: 'UA', expectedBucket: 'keeftalk-eeur-private' },
    ENAM: { country: 'US', expectedBucket: 'keeftalk-enam-private' },
    APAC: { country: 'JP', expectedBucket: 'keeftalk-apac-private' },
    OC: { country: 'AU', expectedBucket: 'keeftalk-oc-private' }
  }

  for (const [region, data] of Object.entries(regions)) {
    assertEquals(isBucketProvisioned(data.expectedBucket), true, `Bucket ${data.expectedBucket} for region ${region} must be recognized as provisioned`)
  }
})

Deno.test('2. Deterministic Vault Object Key Formatting', () => {
  const userId = '36e6874d-cb83-40fa-b74c-e8829491f26e'
  const fileId = 'fb6ac65e-b448-4f4d-ad3d-ebec43cd3941'
  const country = 'TN'

  const keyOriginal = buildVaultObjectKey(country, userId, 'files', fileId, 'original')
  assertEquals(keyOriginal, 'TN/36e6874d-cb83-40fa-b74c-e8829491f26e/files/fb6ac65e-b448-4f4d-ad3d-ebec43cd3941/original')

  const keyThumb = buildVaultObjectKey(country, userId, 'files', fileId, 'thumbnail')
  assertEquals(keyThumb, 'TN/36e6874d-cb83-40fa-b74c-e8829491f26e/files/fb6ac65e-b448-4f4d-ad3d-ebec43cd3941/thumbnail')
})

Deno.test('3. Logical Vault Storage Namespaces', () => {
  const userId = 'user_123'
  const fileId = 'file_456'
  const country = 'TN'

  const supportedCategories = ['archives', 'exports', 'files', 'taging']
  for (const cat of supportedCategories) {
    const key = buildVaultObjectKey(country, userId, cat, fileId, 'original')
    assertEquals(key, `TN/user_123/${cat}/file_456/original`)
  }

  // Invalid category falls back to 'files'
  assertEquals(validateCategory('invalid_folder'), 'files')
})

Deno.test('4. Missing Regional Bucket Detection', () => {
  assertFalse(isBucketProvisioned('keeftalk-nonexistent-private'), 'Non-existent buckets must fail provisioning check')
  assertFalse(isBucketProvisioned('supabase-storage-bucket'), 'Arbitrary buckets must fail provisioning check')
})

Deno.test('5. Idempotency of Vault Key Generation', () => {
  const key1 = buildVaultObjectKey('TN', 'user1', 'files', 'file123', 'original')
  const key2 = buildVaultObjectKey('TN', 'user1', 'files', 'file123', 'original')
  assertEquals(key1, key2, 'Key generation must be deterministic and idempotent')
})

Deno.test('6. Vault Operations Do NOT Call Supabase Storage API', () => {
  const bucket = 'keeftalk-weur-private'
  const path = 'TN/user1/files/file123/original'
  const url = getR2Url(bucket, path)

  assertStringIncludes(url.toString(), 'r2.cloudflarestorage.com', 'Vault operations must use Cloudflare R2 endpoint')
  assertFalse(url.toString().includes('/storage/v1/object/'), 'Vault operations must NEVER use Supabase Storage /storage/v1/object/ path')
})

Deno.test('7. AwsClient Pre-signed PUT URL Signing', async () => {
  const r2 = new AwsClient({
    accessKeyId: 'mock_access_key',
    secretAccessKey: 'mock_secret_key',
    service: 's3',
    region: 'auto'
  })

  const targetUrl = getR2Url('keeftalk-weur-private', 'TN/user1/files/file123/original')

  const signedRequest = await r2.sign(targetUrl.toString(), {
    method: 'PUT',
    aws: { signQuery: true }
  })

  assertStringIncludes(signedRequest.url, 'X-Amz-SignedHeaders=host')
  assertFalse(signedRequest.url.includes('x-amz-content-sha256'), 'SignedHeaders should only require host for maximum client compatibility')
})
