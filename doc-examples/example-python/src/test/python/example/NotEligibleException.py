from java.lang import RuntimeException


class NotEligibleException(RuntimeException):

    def __init__(self, message: str):
        super().__init__(message)
