from nltk.util import ngrams
import os
from Documents import Documents
import sys
from random import randint
from htmlParse import parseHTMLContents

class DuplicateDetector:	
	def __init__(self):
		self.documents = []
		#<docId, list-of-shingle-tuples-of-length-k-for-doc>
		self.docIdShinglesDict = {}
		self.docNum = 0
		#<docID, doc>
		self.idDocDict = {}
		#<docID, list-of-hashed-shingles-for-doc>
		self.docIdShinglesHashDict = {}
		self.permutationCount = {}
		#<hash-value, permutation-value-for-hash-value>
		self.permutationForHashValDict = {}

	def readDirectoryContents(self, path):
		parseHTMLObj = parseHTMLContents()
		for fileName in os.listdir(path):
			# print fileName
			self.docNum += 1
			name = fileName
			with open(path+"/"+fileName, 'r') as fileRead:
				contents = fileRead.read()
			if fileName.endswith(".html"):
				contents = self.htmlParse(parseHTMLObj, contents)
			doc = Documents(name, self.docNum, contents)
			self.idDocDict[self.docNum] = doc
			self.documents.append(doc)


	def htmlParse(self, parseHTMLObj, content):
	#calls parse() class parseHTMLContents
		return parseHTMLContents.parse(parseHTMLObj, content)

	def generateShinglesForDocument(self, content, k):
	#k indicates no. of  words in a shingle
		words = content.split()
		kGrams = ngrams(words, k)
		return list(kGrams)

	def populateDocIDShingleWords(self, k):
#gets the shingles (words) as a list for a doc and adds it to the dict as <doc, list-of-shingles>
		for doc in self.documents:
			docContent = doc.contentString
			docName = doc.name
			docId = doc.Id
			shinglesList = self.generateShinglesForDocument(docContent, k)
			self.docIdShinglesDict[docId] = shinglesList
	
	def naiveTempHashFunction(self, shingleStr):
#returns a hash of the shingle passed - naive Python hash(); change, if required
		return hash(shingleStr)

	def combineShingleTupleElementsIntoString(self, shingleTuple):
#('I', 'am', 'a', 'shingle') => I am a shingle (input to the hash function)
		shingleStr = ' '.join(shingleTuple)
		return shingleStr

	def generateHashOfShingleTuple(self, shingleTup):
#convert a shingle-tuple into a shingle-string and return its hashed value
		shingleStr = self.combineShingleTupleElementsIntoString(shingleTup)
		shingleHash = self.naiveTempHashFunction(shingleStr)
		return shingleHash

	def convertShinglesToHashNumbersForDoc(self):
#pass a list of tupled shingles for each doc through hash fn and get hash-coded values of shingles in an array for each doc
		for docId in self.docIdShinglesDict:
			shinglesTupleList = self.docIdShinglesDict[docId]
			#create empty array to hold the hashed values
			hashedShingles = []
			for shingleTuple in shinglesTupleList:
				hashOfShingleTup = self.generateHashOfShingleTuple(shingleTuple)
				hashedShingles.append(hashOfShingleTup)
			self.docIdShinglesHashDict[docId] = hashedShingles

	def testForDups(self):
		numPerms = 200;
		for p in xrange(numPerms):
			self.nextPermutation()
		for i in self.idDocDict:
			for j in self.idDocDict:
				if i == j:
					continue
				countSimShingles = self.retrieve(i,j)
				iDocName = self.idDocDict[i].name
				jDocName = self.idDocDict[j].name
				#Uncomment this if you want to check number of common shingles between 2 docs
				#print "Count of similar shingles of "  + iDocName + " with " + jDocName + " = " + str(countSimShingles)
				if (countSimShingles > (0.6 * numPerms)):
					#change the next few lines to return something useful, like a boolean val indicating dups
					if iDocName < jDocName:
						print "%s,%s" % (iDocName, jDocName)
					else:
						print "%s,%s" % (jDocName, iDocName)
					#print "Docs: " + iDocName + " " + jDocName + " are duplicates.\n"

	def nextPermutation(self):
		#same as x in notes
		minPermValDocsDict = {}
		#same as H in notes
		shingleHashPermutationDict = {}
		#permSet stores the computed permutations so that these are not repeated; can be omitted as probability of collision is low, but I've retained it just to be sure there aren't any collisions/repetitions in permutation values for the hashes
		permSet = set()
		self.permutationForHashValDict = {}
		for docId in self.idDocDict:
			hashedShinglesForDoc = self.docIdShinglesHashDict[docId]	
			#hashVal same as Q in notes
			for hashVal in hashedShinglesForDoc:
				#same as P in notes
				permForHashVal = self.returnPermutationForHashValue(hashVal, permSet)
				if docId not in minPermValDocsDict:
					minPermValDocsDict[docId] = permForHashVal
				else:
					minPermValForDocSoFar = minPermValDocsDict[docId]
					#update minimum permutation value for document, if need be
					if (permForHashVal < minPermValForDocSoFar):
						minPermValDocsDict[docId] = permForHashVal
		#same as p2Docs in notes
		p2Docs = {}

		#begin 2nd half of func in notes
		#docId = I in notes
		for docId in self.idDocDict:
			minValShingle = minPermValDocsDict[docId]
			try:
				L = p2Docs[minValShingle]
			except KeyError, e:
				L = []
			for J in L:
				C = self.retrieve(docId, J)
				if C == -1 or C == -2:
					C = 0
				self.store(docId, J, C + 1)
			L.append(docId)
			p2Docs[minValShingle] = L


	def store(self, i, j, newCount):
		#if doesn't exist, create new dict
		#if exists, update value
		if i not in self.permutationCount:
			newJEntry = {}
			newJEntry[j] = newCount
			self.permutationCount[i] = newJEntry
			return
		JEntry = self.permutationCount[i]
		JEntry[j] = newCount
		self.permutationCount[i] = JEntry
		#check if above line can be omitted

	def retrieve(self, i, j):
#retrieve the count of matched/common minimum-permuted shingles between two docs, i and j, from permutationCount dict. If i and j have absolutely no min-perm shingle value in common, return -1 or -2.
		try:
			dictDocI =  self.permutationCount[i]
		except KeyError, e:
			return -1
		try:
			countOfDocIAndDocJCommonShingles = dictDocI[j]
		except KeyError, e2:
			return -2
		return countOfDocIAndDocJCommonShingles

	
	def returnPermutationForHashValue(self, hashVal, permSet):
#returns the permuted mapping, if present in dict, of the hash value. If not present, calculates and stores the permutation unique to this particular hash value in dict and returns that.
#For now, just return the hash value as is - dumb permutation function until prof explains the purpose of this.
		try:
			newPermVal = self.permutationForHashValDict[hashVal]
		except KeyError, e:
		#compute perm for this hashVal and store in dict
			newPermVal = self.getPermValForHashVal(permSet, hashVal)
			self.permutationForHashValDict[hashVal] = newPermVal
		return newPermVal


	def getPermValForHashVal(self, permSet, hashVal):
		#computes a new permutation value for the unseen hash value (perm value is unique for every hash value)
		#just generate a new permutation number to this hashVal. check if this newpermnumber is available or taken (permSet)
		#initialize hashVal for loop entry
		perm = -1
		while perm == -1 or perm in permSet:
			perm = randint(0, sys.maxint)
		permSet.add(perm)
		return perm

def main(argv):
	input_dir = '/home/es2697/wse-final/test-sample'
	if len(argv) > 1:
		input_dir = argv[1]
	dd = DuplicateDetector() 
	dd.readDirectoryContents(input_dir)	
	dd.populateDocIDShingleWords(4)
	dd.convertShinglesToHashNumbersForDoc()
	dd.testForDups()


if __name__ == "__main__":
	main(sys.argv)
