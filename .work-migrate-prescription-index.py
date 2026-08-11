import os

from pymongo import ASCENDING, MongoClient


client = MongoClient(os.environ["MONGODB_URI"])
database = client.get_default_database()
collection = database["prescriptions"]
duplicates = list(collection.aggregate([
    {"$match": {"medicalRecordId": {"$ne": None}}},
    {"$group": {"_id": "$medicalRecordId", "count": {"$sum": 1}}},
    {"$match": {"count": {"$gt": 1}}},
]))
print(f"DUPLICATE_GROUPS={len(duplicates)}")
if duplicates:
    raise SystemExit("Duplicate prescriptions exist; index was not changed.")

index = collection.index_information().get("medicalRecordId")
if index and not index.get("unique", False):
    collection.drop_index("medicalRecordId")
collection.create_index(
    [("medicalRecordId", ASCENDING)],
    name="medicalRecordId",
    unique=True,
)
print("UNIQUE_INDEX_READY=medicalRecordId")
