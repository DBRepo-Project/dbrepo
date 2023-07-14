import UserMapper from '@/api/user.mapper'

class DatabaseMapper {
  databaseToOwner (database) {
    if (!database) {
      return null
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
