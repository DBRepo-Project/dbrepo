import json

from dbrepo.api.dto import Timestamp


class OpenSearchEncoder(json.JSONEncoder):
    """
    Utility class for encoding the timestamp to ISO 8601 format that is needed by Open Search.
    """

    def default(self, obj):
        if isinstance(obj, Timestamp):
            return obj.isoformat()
        return super(OpenSearchEncoder, self).default(obj)
