class DatabaseUtils {
  isOwner (database, user) {
    if (!database || !user) {
      return false
    }
    return database.owner.id === user.id
  }
}

export default new DatabaseUtils()
