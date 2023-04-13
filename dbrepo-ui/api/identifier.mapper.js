class IdentifierMapper {
  identifierToCreators (identifier) {
    if (!identifier) {
      return null
    }
    const creators = identifier.creators
    let str = ''
    for (let i = 0; i < creators.length; i++) {
      /* separator */
      if (creators.length > 1 && i === creators.length - 1) {
        str += ', & '
      } else if (i > 0 && creators.length !== 2) {
        str += ', '
      }
      /* name */
      if (creators[i].firstname) {
        str += (creators[i].firstname.toUpperCase().substring(0, 1) + '., ')
      }
      if (creators[i].lastname) {
        str += creators[i].lastname
      }
    }
    return str
  }
}

export default new IdentifierMapper()
