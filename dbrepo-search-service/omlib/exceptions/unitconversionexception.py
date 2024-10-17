class UnitConversionException(Exception):

    def __init__(self, message):
        self.message = message


class ScaleConversionException(Exception):

    def __init__(self, message):
        self.message = message
