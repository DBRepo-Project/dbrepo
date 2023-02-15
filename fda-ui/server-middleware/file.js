export function uploadRequestHandler (multer) {
  return multer.single('file')
}
