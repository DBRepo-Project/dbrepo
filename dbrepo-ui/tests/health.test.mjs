import test from 'node:test'
import assert from 'node:assert/strict'

test('public health uses the documented JSON availability contract', async () => {
  assert.ok(process.env.ENDPOINT, 'Set ENDPOINT to the running UI or gateway')
  const response = await fetch(new URL('/health', process.env.ENDPOINT), {
    redirect: 'manual', signal: AbortSignal.timeout(10000)
  })
  assert.equal(response.status, 200)
  assert.match(response.headers.get('content-type') ?? '', /^application\/json\b/i)
  assert.deepEqual(await response.json(), { status: 'UP' })
})
