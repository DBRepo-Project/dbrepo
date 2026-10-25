/* unsaved add/edit tuple input, kept in this tab for the signed-in account so it survives the sign-in redirect but is never restored for another account */
export const useTupleDraft = (databaseId: string, tableId: string, row: unknown) => {
  /* the owner is read now, while the user is still signed in, so a later sign-out does not move the draft */
  const { user } = useOidcAuth()
  const provider = user.value?.provider
  const subject = user.value?.userInfo?.sub
  const key = provider && subject ? 'dbrepo-tuple-draft:' + JSON.stringify([provider, subject, databaseId, tableId, row]) : null

  function save(tuple: object): void {
    if (!key) {
      return
    }
    try {
      sessionStorage.setItem(key, JSON.stringify(tuple))
    } catch (error) {
      console.warn('Failed to save tuple draft', error)
    }
  }

  function restore(): object | null {
    if (!key) {
      return null
    }
    try {
      const draft = sessionStorage.getItem(key)
      return draft ? JSON.parse(draft) : null
    } catch (error) {
      console.warn('Failed to restore tuple draft', error)
      return null
    }
  }

  function clear(): void {
    if (!key) {
      return
    }
    try {
      sessionStorage.removeItem(key)
    } catch (error) {
      console.warn('Failed to clear tuple draft', error)
    }
  }

  return {save, restore, clear}
}
