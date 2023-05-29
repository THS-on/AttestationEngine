# Copyright 2021 Nokia
# Licensed under the BSD 3-Clause Clear License.
# SPDX-License-Identifier: BSD-3-Clear

import pymongo
import a10.asvr.db.configuration

"""This module is used to communicate with MongoDB. 
   As long as it matches the semantics of the functions, replacement of this file can be used to interface 
   with any database system
"""

asclient = pymongo.MongoClient(a10.asvr.db.configuration.MONGODBURL)
asdb = asclient[a10.asvr.db.configuration.MONGODBNAME]


##################################################
#
# Generics
#
##################################################


def getDatabaseStatus():
    """ Returns information on the state of the database

    :return: a structure containing information about the number of items stored in the database and other meta-data
    :rtype: dict

    """

    dbstatus = {}

    for c in [
        "elements",
        "policies",
        "expectedvalues",
        "claims",
        "results",
        "sessions",
        "hashes",
        "pcrschemas",
        "log",
    ]:
        collection = asdb[c]
        count = collection.estimated_document_count()
        dbstatus[c] = str(count)

    dbstatus["elements_current"] = len(getElements(archived=False))
    dbstatus["elements_archived"] = len(getElements(archived=True))

    return dbstatus


##################################################
#
# Logging
#
##################################################


def writeLogEntry(t, ch, op, data):
    """ Writes an entry to the logging table

    :params str t: timestamp
    :params str ch: channel
    :params str op: operation
    :params dict data: associated data
    """

    collection = asdb["log"]

    e = {"t": t, "ch": ch, "op": op, "data": data}

    r = collection.insert_one(e)


def getLatestLogEntries(n):
    """ Returns the latest log entries 

    :params int n: number of entries to return
    :returns: list of log entries
    :rtype: list dict
    """

    collection = asdb["log"]
    ls = collection.find({}, {"_id": False}).sort("t", pymongo.DESCENDING)
    return list(ls)


def getLogEntryCount():
    """ Returns the number of log entries

    :returns: number of log entries
    :rtype: int
    """
    collection = asdb["log"]
    return collection.estimated_document_count()


def getLogEntriesForItemID(i):
    """ Returns the log entries with a field
         data.itemid == i

    :params str i: itemid
    :returns: list of log entries
    :rtype: list dict
    """
    collection = asdb["log"]
    ls = collection.find({"data.itemid": i}, {"_id": False}).sort("t", pymongo.DESCENDING)
    return list(ls)   

def getLogFull():
    collection = asdb["log"]
    e = collection.find({}, {"_id": False})
    return list(e)
##################################################
#
# Sessions
#
##################################################

def openSession(s):
    """
    Opens a session and returns an itemid for that session
    """
    collection = asdb["sessions"]
    r = collection.insert_one(s)

    if r.inserted_id == None:
        return False
    else:
        return True

def closeSession(s):
    """
    Closes a session
    """
    collection = asdb["sessions"]
    r = collection.update_one({"itemid": s["itemid"]}, {"$set": s})

    if r.matched_count == 1:
        return True
    else:
        return False


def getSessions(closed=False):
    """
    Returns all - by default - open sessions
    """
    collection = asdb["sessions"]
    es = collection.find({ "closed":{"$exists":closed}}, {"_id": False, "itemid": True})
    return list(es)

def getSession(s):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["sessions"]
    e = collection.find_one({"itemid": s}, {"_id": False})
    return e

#These need to be modified to ensure we do not update closed sessions
def associateClaim(s,c):
    collection = asdb["sessions"]
    e = collection.update_one({"itemid": s}, {'$push': {'claims': c}})
    if e.matched_count == 1:
        return True
    else:
        return False


def associateResult(s,r):
    collection = asdb["sessions"]
    e = collection.update_one({"itemid": s}, {'$push': {'results': r}})
    if e.matched_count == 1:
        return True
    else:
        return False


def associateSession(s,ss):
    collection = asdb["sessions"]
    e = collection.update_one({"itemid": s}, {'$push': {'sessions': ss} })
    if e.matched_count != 1:
        return False
    
    e = collection.update_one({"itemid": ss}, {'$set': {'parentSession':s}})
    if e.matched_count == 1:
        return True
    else:
        return False


##################################################
#
# Elements
#
##################################################


def addElement(e):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """

    collection = asdb["elements"]
    r = collection.insert_one(e)

    if r.inserted_id == None:
        return False
    else:
        return True


def getElement(i):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["elements"]
    e = collection.find_one({"itemid": i}, {"_id": False})
    return e


def getElementByName(n):
    """ Returns an element with the given name 
     ONLY from the unarchived list ... 

    :param str n: name of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["elements"]
    e = collection.find_one({"name": n,
                             "archived":{"$exists":False}
          }, {"_id": False})
    return e


def getElements(archived=False):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["elements"]
    e = collection.find({ "archived":{"$exists":archived}}, {"_id": False, "itemid": True})
    return list(e)


def getElementsByType(t,archived=False):
    """ Returns an element with the given itemid

    :param str t: The type to filter by
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["elements"]
    es = list(collection.find({ "archived":{"$exists":archived}}, {"_id": False, "itemid": True, "type": True}))
    print("\nES with TYPE is ", type(es), es,"\n")
    fs = list(filter(
            lambda x: t in x["type"], es
        ))
    print("\nFS ",fs,"\n")
    return list(fs)


def getElementsFull(archived=False):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["elements"]
    e = collection.find({ "archived":{"$exists":archived}}, {"_id": False})
    return list(e)


def deleteElementPermanently(e):
    collection = asdb["elements"]
    r = collection.delete_one({"itemid": e})

    if r.deleted_count == 1:
        return True
    else:
        return False


def deleteElement(e,t):
    """
        e is an element id
        t is a timestamp
    """
    collection = asdb["elements"]

    u = { "archived": t }

    r = collection.update_one({"itemid": e}, {"$set": u})

    if r.matched_count == 1:
        return True
    else:
        return False


def updateElement(e):
    collection = asdb["elements"]
    r = collection.replace_one({"itemid": e["itemid"]}, e)

    if r.matched_count == 1:
        return True
    else:
        return False


##################################################
#
# Policies
#
##################################################


def addPolicy(e):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["policies"]

    r = collection.insert_one(e)

    if r.inserted_id == None:
        return False
    else:
        return True


def getPolicy(i):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["policies"]
    e = collection.find_one({"itemid": i}, {"_id": False})
    return e


def getPolicyByName(n):
    """ Returns a policy with the given name

    :param str n: name of the policy
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["policies"]
    e = collection.find_one({"name": n}, {"_id": False})
    return e


def getPolicies():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["policies"]
    e = collection.find({}, {"_id": False, "itemid": True})
    return list(e)


def getPoliciesFull():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["policies"]
    e = collection.find({}, {"_id": False})
    return list(e)


def deletePolicy(i):
    collection = asdb["policies"]
    r = collection.delete_one({"itemid": i})

    if r.deleted_count == 1:
        return True
    else:
        return False


def updatePolicy(e):
    collection = asdb["policies"]
    r = collection.replace_one({"itemid": e["itemid"]}, e)

    if r.matched_count == 1:
        return True
    else:
        return False


##################################################
#
# Hashes
#
##################################################


def addHash(h):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["hashes"]
    r = collection.insert_one(h)

    if r.inserted_id == None:
        return False
    else:
        return True


def getHash(h):
    """ Returns an element with the given itemid

    :param str h: the hash to search for
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["hashes"]
    e = collection.find_one({"hash": h}, {"_id": False})
    return e


def getHashes():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["hashes"]
    e = collection.find({}, {"_id": False, "hash": True})
    return list(e)


def getHashesFull():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["hashes"]
    e = collection.find({}, {"_id": False})
    return list(e)




def deleteHash(h):
    collection = asdb["hashes"]
 
    r = collection.delete_one({"hash": h})

    if r.deleted_count == 1:
        return True
    else:
        return False


    if r.matched_count == 1:
        return True
    else:
        return False


def updateHash(h):
    collection = asdb["hashes"]
    r = collection.replace_one({"hash": h["hash"]}, h)

    if r.matched_count == 1:
        return True
    else:
        return False


##################################################
#
# Expected Values
#
##################################################


def addExpectedValue(e):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["expectedvalues"]

    r = collection.insert_one(e)

    if r.inserted_id == None:
        return False
    else:
        return True


def getExpectedValue(i):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["expectedvalues"]
    e = collection.find_one({"itemid": i}, {"_id": False})
    return e


def getExpectedValues():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["expectedvalues"]
    e = collection.find({}, {"_id": False, "itemid": True})
    return list(e)


def getExpectedValuesFull():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["expectedvalues"]
    e = collection.find({}, {"_id": False})
    return list(e)


def getExpectedValuesForElement(i):
    """ Returns a expected values for given elementID

    :param str i: ItemID of the element
    :return: the returned objects from Monogo less the mongo object ID
    :rtype: list
    """

    collection = asdb["expectedvalues"]
    e = collection.find({"elementID": i}, {"_id": False})
    return list(e)


def getExpectedValuesForPolicy(i):
    """ Returns a expected values for given policyID

    :param str i: ItemID of the element
    :return: the returned objects from Monogo less the mongo object ID
    :rtype: list
    """

    collection = asdb["expectedvalues"]
    e = collection.find({"policyID": i}, {"_id": False})
    return list(e)


def getExpectedValueForElementAndPolicy(e, p):
    """ Returns a expected value for given elementID and policyID

    :param str e: ItemID of the element
    :param str p: ItemID of the policy    
    :return: the returned objects from Monogo less the mongo object ID
    :rtype: list
    """

    collection = asdb["expectedvalues"]
    e = collection.find_one({"elementID": e, "policyID": p}, {"_id": False})
    return e


def deleteExpectedValue(i):
    collection = asdb["expectedvalues"]
    r = collection.delete_one({"itemid": i})

    if r.deleted_count == 1:
        return True
    else:
        return False


def updateExpectedValue(e):
    collection = asdb["expectedvalues"]
    r = collection.replace_one({"itemid": e["itemid"]}, e)

    if r.matched_count == 1:
        return True
    else:
        return False


##################################################
#
# Claims
#
##################################################


def addClaim(e):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["claims"]
    print("\n*************\n",e,"\n=========\n")
    r = collection.insert_one(e)

    if r.inserted_id == None:
        return False
    else:
        return True


def getClaim(i):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["claims"]
    e = collection.find_one({"itemid": i}, {"_id": False})
    return e


def getClaims(n=1000):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :param int n: number of claims to obtain
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["claims"]
    e = collection.find({}, {"_id": False, "itemid": True}).sort(
        "header.as_requested", pymongo.DESCENDING
    ).limit(n)
    return list(e)


def getClaimsFull(n):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["claims"]
    e = (
        collection.find({}, {"_id": False})
        .sort("header.as_requested", pymongo.DESCENDING)
        .limit(n)
    )
    return list(e)




def getClaimsForElement(eid,n=1000):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :param int n: number of claims to obtain
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["claims"]
    e = collection.find({"header.element.itemid":eid}, {"_id": False, "itemid": True}).sort(
        "header.as_requested", pymongo.DESCENDING
    ).limit(n)
    return list(e)


def getClaimsFullForElement(eid,n):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["claims"]
    e = collection.find({"header.element.itemid":eid}, {"_id": False}).sort("header.as_requested", pymongo.DESCENDING).limit(n)
    
    return list(e)





def getAssociatedResults(i):
    """ Returns the set of results associated with the given claim

    :param str i: ItemID of the claim
    :return: a list of result item IDs
    :rtype: list 
    """

    collection = asdb["results"]
    rs = collection.find({"claimID": i}, {"_id": False}).sort(
        "verifiedAt", pymongo.DESCENDING
    )
    return list(rs)


##################################################
#
# Results
#
##################################################


def addResult(e):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["results"]

    r = collection.insert_one(e)

    if r.inserted_id == None:
        return False
    else:
        return True


def getResult(i):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["results"]
    e = collection.find_one({"itemid": i}, {"_id": False})
    return e


def getResults(n=500):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["results"]
    e = collection.find({}, {"_id": False, "itemid": True}).sort("verifiedAt", pymongo.DESCENDING).limit(n)
    return list(e)

def getResultsSince(t):
    
    """ Returns results since t timestamp

    :param str t: float timestamp
    :return: the list of results
    :rtype: list dict or None
    """
    
    out = []

    collection = asdb["results"]
    e = collection.find({}, {'_id': False,'itemid':True,'claimID':True,'result':True,'elementID':True,'policyID':True,'ruleName':True,'verifiedAt':True}).sort("verifiedAt", pymongo.DESCENDING)
    ## Mongodb (3.6) does not support casting string verifiedAt for comparison with timestamp, filtering must be done manually. 

    for i in e:
        if(i["verifiedAt"] != None):
            if( float(i["verifiedAt"]) > t):
                out.append(i)
            else:
                return out
    return out

def getResultsFull(n):
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["results"]
    e = (
        collection.find({}, {"_id": False})
        .sort("verifiedAt", pymongo.DESCENDING)
        .limit(n)
    )
    return list(e)


def getLatestResults(e, n=500):
    """ Returns the latest n results for a given element sorted by verifiedAt.
          We let the underlying database to do the sorting for efficiency reasons.

    :param str e: ItemID of the element
    :param int n: Maximum number of items to return, defaults to 10
    :return: the list of results
    :rtype: list dict or None
    """

    collection = asdb["results"]
    rs = list(
        collection.find({"elementID": e},{'_id': False})
        .sort("verifiedAt", pymongo.DESCENDING)
        .limit(n)
    )
    return rs


def getLatestResultsForElementAndPolicy(e, p, n):
    """ Returns the latest n results for a given element sorted by verifiedAt.
          We let the underlying database to do the sorting for efficiency reasons.

    :param str e: ItemID of the element
    :param str p: ItemID of the policy    
    :param int n: Maximum number of items to return, defaults to 10
    :return: the list of results
    :rtype: list dict or None
    """

    collection = asdb["results"]
    rs = list(
        collection.find({"elementID": e, "policyID": p})
        .sort("verifiedAt", pymongo.DESCENDING)
        .limit(n)
    )
    return rs



##################################################
#
# PCR Schemas
#
##################################################


def addPCRSchema(h):
    """ Adds an entry to the elements collection.

    First this outputs the element as JSON and then tries to insert it into the database.
    MongoDB returns an inserted_id - which is a mongo ObjectID - if this is successful.

    :param dict e: the element to be added
    :return: the success or failure of the operation
    :rtype: Bool


    """
    collection = asdb["pcrschemas"]
    r = collection.insert_one(h)

    if r.inserted_id == None:
        return False
    else:
        return True


def getPCRSchema(h):
    """ Returns an element with the given itemid

    :param str h: the hash to search for
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["pcrschemas"]
    e = collection.find_one({"name": h}, {"_id": False})
    return e


def getPCRSchemas():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["pcrschemas"]
    e = collection.find({}, {"_id": False, "itemid": True, "name": True})
    return list(e)


def getPCRSchemasFull():
    """ Returns an element with the given itemid

    :param str i: ItemID of the element
    :return: the returned object from Monogo less the mongo object ID
    :rtype: dict or None
    """

    collection = asdb["pcrschemas"]
    e = collection.find({}, {"_id": False})
    return list(e)
