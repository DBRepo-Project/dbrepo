import UserMapper from '@/api/user.mapper'
import DatabaseMapper from '@/api/database.mapper'

class ContainerMapper {
  containerToCreator (container) {
    if (!container) {
      return null
    }
    if (container.database) {
      return DatabaseMapper.databaseToOwner(container.database)
    }
    return UserMapper.userToFullName(container.creator)
  }
}

export default new ContainerMapper()
