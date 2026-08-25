from dotenv import load_dotenv
import os


load_dotenv()

class Settings:

    API_KEY = os.getenv("API_KEY")

    BASE_URL = os.getenv("BASE_URL")

    MODEL = os.getenv("MODEL")

    EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL")
    


settings = Settings()