class DashboardNotFound(Exception):
    """
    The dashboard could not be found.
    """
    pass


class DashboardAccessError(Exception):
    def __init__(self, message: str, status_code: int = 502):
        super().__init__(message)
        self.status_code = status_code
