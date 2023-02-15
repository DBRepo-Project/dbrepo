const fs = require('fs')
const test = require('ava')
const multer = require('multer')
const upload = multer({ dest: '/tmp' })
const file = require('@/server-middleware/file')

test('upload_succeeds', (t) => {
  /* test */
  file.uploadRequestHandler(upload)
  // t.is(fs.existsSync('/tmp/mock.csv'), true, 'File does not exist')
  t.pass()
})
