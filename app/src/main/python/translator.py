# Copyright 2024 anyone-Hub
from googletrans import Translator

def translate_text(text: str, target_lang: str = 'es') -> str:
    """
    Translates input text into the target language using googletrans.
    Defaults to Spanish ('es').
    """
    try:
        translator = Translator()
        result = translator.translate(text, dest=target_lang)
        return result.text
    except Exception as e:
        return f"Translation Error: {str(e)}"
