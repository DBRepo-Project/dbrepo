import assert from 'node:assert/strict'

const endpoint = new URL(process.env.ENDPOINT || 'http://localhost')
const get = path => fetch(new URL(path, endpoint), {
  redirect: 'manual', signal: AbortSignal.timeout(10000),
})

for (const path of process.argv.slice(2)) {
  const response = await get(path)
  assert.equal(response.status, 200, `${path} returned HTTP ${response.status}`)
  assert.match(response.headers.get('content-type') ?? '', /^text\/html\b/i)
  assert.match(await response.text(), /\bid=["']__nuxt["']/, `${path} did not render the Nuxt application`)
}

async function redirect(path) {
  const response = await get(path)
  await response.body?.cancel()
  assert.equal(response.status, 302, `${path} returned HTTP ${response.status}`)
  const location = response.headers.get('location')
  assert.ok(location, `${path} returned no redirect location`)
  return new URL(location, endpoint)
}

for (const action of ['login', 'logout']) {
  const target = await redirect(`/auth/${action}`)
  assert.equal(target.href, new URL(`/auth/keycloak/${action}`, endpoint).href)
}
const authorization = await redirect('/auth/keycloak/login')
assert.match(authorization.pathname, /\/protocol\/openid-connect\/auth$/)
assert.equal(authorization.searchParams.get('response_type'), 'code')
assert.ok(authorization.searchParams.get('client_id'))
