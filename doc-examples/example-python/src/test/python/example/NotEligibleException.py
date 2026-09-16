from java.lang import RuntimeException


class NotEligibleException(RuntimeException):

    # TODO(python): the class generated for the exception does not pass the message of the Python exception to
    # RuntimeException, so it is kept in an attribute, which the generated class serves as getMessage()
    message: str

    def __init__(self, message: str):
        super().__init__(message)
        self.message = message
