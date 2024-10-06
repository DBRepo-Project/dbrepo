export default defineEventHandler((event) => {
  event.node.res.setHeader('Content-Type', 'text/plain');
  return 'service_started 1'
})
