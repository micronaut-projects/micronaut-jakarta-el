from micronaut.context.annotation import Executable
from micronaut.el.annotation import ELFunction


class TextFunctions:

    @staticmethod
    @Executable
    @ELFunction  # <1>
    def shout(text: str) -> str:
        return text.upper() + "!"

    @staticmethod
    @Executable
    @ELFunction("initials")  # <2>
    def initialsOf(text: str) -> str:
        return "".join(word[0] for word in text.split(" "))
