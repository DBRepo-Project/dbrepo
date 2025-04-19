class ResponseCodeError(Exception):
    """
    The response code is different from the expected one.
    """
    pass


class UsernameExistsError(Exception):
    """
    The username is already in use by another user.
    """
    pass


class EmailExistsError(Exception):
    """
    The e-mail address is already in use by another user.
    """
    pass


class NameExistsError(Exception):
    """
    The resource with this name exists.
    """
    pass


class NotExistsError(Exception):
    """
    The resource was not found.
    """
    pass


class ForbiddenError(Exception):
    """
    The action is not allows.
    """
    pass


class MalformedError(Exception):
    """
    The data is malformed.
    """
    pass


class QueryStoreError(Exception):
    """
    The data is malformed.
    """
    pass


class MetadataConsistencyError(Exception):
    """
    The service expected metadata that is not present.
    """
    pass


class FormatNotAvailable(Exception):
    """
    The service cannot provide the result in the requested representation.
    """
    pass


class ExternalSystemError(Exception):
    """
    The service could not communicate with the external system.
    """
    pass


class AuthenticationError(Exception):
    """
    The action requires authentication.
    """
    pass


class ServiceConnectionError(Exception):
    """
    The service failed to establish connection.
    """
    pass


class ServiceError(Exception):
    """
    The service failed to perform the requested action.
    """
    pass


class UploadError(Exception):
    """
    The upload was not successful.
    """
    pass


class RequestError(Exception):
    """
    The request cannot be sent.
    """
    pass
