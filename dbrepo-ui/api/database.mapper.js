import UserMapper from '@/api/user.mapper'
import IdentifierMapper from '@/api/identifier.mapper'

class DatabaseMapper {
  databaseToOwner (database) {
    if (!database) {
      return null
    }
    if (database.identifier) {
      return IdentifierMapper.identifierToCreators(database.identifier)
    }
    return UserMapper.userToFullName(database.owner)
  }

  databaseToContact (database) {
    if (!database) {
      return null
    }
    return UserMapper.userToFullName(database.contact)
  }
}

export default new DatabaseMapper()
