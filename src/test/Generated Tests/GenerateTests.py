import string
import random
from typing import Final
import GenerateComponent as gc
from itertools import product
from string import ascii_letters
import os, shutil

class GenerateTest:
    def __init__(self):
        self.alphabets = [''.join(i) for i in product(ascii_letters, repeat = 1)]
        self.numOfEachActs: Final[int] = 1
        self.minStates: Final[int] = 9
        self.maxStates: Final[int] = 11
        # self.minComponents: Final[int] = 3
        # self.maxComponents: Final[int] = 9
        self.componentCounter = 0
        self.experimentInput = ''
        
        self.POINT_TO_POINT: Final[string] = 'P2P'
        self.MESH: Final[string] = 'Mesh'
        self.STAR: Final[string] = 'Star'
        self.RING: Final[string] = 'Ring'
        self.BUS: Final[string] = 'Bus'
        self.BIPARTITE: Final[string] = 'Bipartite'
        f = open("Configs/Generated Tests.txt", "r")
        self.testType = f.readline().replace('\n', '')
        self.numOfComponents = int (f.readline())
        self.numOfTests = int(f.readline())
        
        self.TYPES: Final[list] = [self.POINT_TO_POINT, self.MESH,
                                   self.STAR, self.RING, self.BUS, self.BIPARTITE]
        self.TYPE_FUNCS: Final[list] = [self.generatePointTPoint, self.generateMesh,
                                        self.generateStar, self.generateRing,
                                        self.generateBus, self.generateBipartite]
        
    def generateSynchComponents(self, synchActions, numOfComponents, type, testCounter, allComponentsCount):
        for i in range(numOfComponents):
            self.componentCounter += 1
            self.experimentInput += ""
            unsynchActs = self.generateActs()
            if(type != self.MESH):
                numOfStates = random.randint(2, 5)
            else:
                numOfStates = random.randint(3, 6)
                
            
            componentGenerator = gc.ComponentGenerator(synchActions, unsynchActs, numOfStates)
            graphString = componentGenerator.generate()

            currentFile = 'src/test/Generated Tests/resources/' + type + '/' + str(testCounter) +\
                '/Component' + str(self.componentCounter) + '.dot'
            self.writeIntoFile(currentFile, graphString)
            
            self.experimentInput += currentFile + '\n'
        
        self.writeIntoFile('src/test/Generated Tests/data/' + type + '/' + str(testCounter) + '.txt', self.experimentInput)
                
    def writeIntoFile(self, file, content, writingType = 'w'):
        with open(file, writingType) as writingfile:
            writingfile.write(content) 
            writingfile.close()   

    def generateAct(self):
        newAct = self.alphabets[random.randint(0, len(self.alphabets) - 1)]
        self.alphabets.remove(newAct)
        return(newAct)
        
    def generateActs(self):    
        acts = list()
        for i in range(self.numOfEachActs):
            acts.append(self.generateAct())
        
        return(acts)
    
    def writeTheInput(self, testCounter, type):
        self.writeIntoFile('src/test/Generated Tests/data/' + type + '-All-Tests.txt',\
            'src/test/Generated Tests/data/' + type + '/' + str(testCounter) + '.txt' + '\n', 'a')   
    
    def generatePointTPoint(self, testCounter):
        self.writeTheInput(testCounter, self.POINT_TO_POINT)

        for twoComponents in range(0, self.numOfComponents - 1, 2):
            synchActs = self.generateActs()
            self.generateSynchComponents(synchActs, 2, self.POINT_TO_POINT, testCounter, self.numOfComponents)
        
        if self.numOfComponents % 2 == 1:
            self.numOfEachActs = 2
            self.generateSynchComponents([], 1, self.POINT_TO_POINT, testCounter, self.numOfComponents)
            self.numOfEachActs = 1
            
    def generateMesh(self, testCounter):
        self.writeTheInput(testCounter, self.MESH)
        
        synchsActs = [0] * self.numOfComponents 
        for i in range(self.numOfComponents):
            synchsActs[i] = [0] * ((self.numOfComponents - 1) * self.numOfEachActs)
        
        for component in range(self.numOfComponents):
            for nextComps in range(component + 1 , self.numOfComponents):
                currentSynchs = self.generateActs()
                currentOutSynchs = [random.randint(0, 1) for i in range(self.numOfEachActs)]
            
                for synchNum in range(len(currentSynchs)):
                    synchsActs[component][((nextComps-1)*self.numOfEachActs) + synchNum] = currentSynchs[synchNum]

                    synchsActs[nextComps][(component*self.numOfEachActs) + synchNum] = currentSynchs[synchNum]
            self.generateSynchComponents(synchsActs[component], 1, self.MESH, testCounter, self.numOfComponents)
            
    def generateStar(self, testCounter):
        self.writeTheInput(testCounter, self.STAR)
        
        centerSynchsActs = []
        for component in range(self.numOfComponents - 1):
            currentSynchs = self.generateActs()
            for synchNum in range(len(currentSynchs)):
                centerSynchsActs.append(currentSynchs[synchNum])
            
            self.generateSynchComponents(currentSynchs, 1, self.STAR, testCounter, self.numOfComponents)
        self.generateSynchComponents(centerSynchsActs, 1, self.STAR, testCounter, self.numOfComponents)
        
    def generateBus(self, testCounter):
        self.writeTheInput(testCounter, self.BUS)
        
        currentSynchs = self.generateActs()
        self.generateSynchComponents(currentSynchs, self.numOfComponents, self.BUS, testCounter, self.numOfComponents)
    
    def generateRing(self, testCounter):
        self.writeTheInput(testCounter, self.RING)
        
        synchsActs = [0] * self.numOfComponents 
        for i in range(self.numOfComponents):
            synchsActs[i] = [0] * (2 * self.numOfEachActs)
                
        for component in range(self.numOfComponents):
            currentSynchs = self.generateActs()
            currentOutSynchs = [random.randint(0, 1) for i in range(self.numOfEachActs)]
            for synchNum in range(len(currentSynchs)):
                if(component == (self.numOfComponents - 1)):
                    nextComp = 0
                else:
                    nextComp = component + 1
                synchsActs[component][self.numOfEachActs + synchNum] = currentSynchs[synchNum]
                
                synchsActs[nextComp][synchNum] = currentSynchs[synchNum]
        
        for component in range(self.numOfComponents):
            self.generateSynchComponents(synchsActs[component], 1, self.RING, testCounter, self.numOfComponents)
            
    def generateBipartite(self, testCounter):
        self.writeTheInput(testCounter, self.BIPARTITE)
        
        synchsActs = [0] * self.numOfComponents 
        for i in range(self.numOfComponents):
            numOfSynchActs = (self.numOfComponents // 2) + int((self.numOfComponents % 2) and (i < (self.numOfComponents // 2)))
            synchsActs[i] = [0] * (numOfSynchActs * self.numOfEachActs)
        
        for component in range(self.numOfComponents):
            if component < self.numOfComponents//2:
                for part2Comp in range(self.numOfComponents//2 , self.numOfComponents):
                    currentSynchs = self.generateActs()
                    currentOutSynchs = [random.randint(0, 1) for i in range(self.numOfEachActs)]
                
                    for synchNum in range(len(currentSynchs)):
                        synchsActs[component][((part2Comp-int(self.numOfComponents/2))*self.numOfEachActs) +\
                                            synchNum] = currentSynchs[synchNum]
                        
                        synchsActs[part2Comp][(component*self.numOfEachActs) + synchNum] = currentSynchs[synchNum]
            self.generateSynchComponents(synchsActs[component], 1, self.BIPARTITE, testCounter, self.numOfComponents)
        
    def resetVars(self, type, testCounter):
        self.clearFolder('src/test/Generated Tests/resources/' + type + '/' + str(testCounter))
        self.alphabets = [''.join(i) for i in product(ascii_letters, repeat = 1)]
        self.experimentInput = ''
        self.componentCounter = 0  
            
    def generateAllTests(self):
        for i in range(self.numOfTests):
            for j in range(len(self.TYPES)):
                if self.TYPES[j] == self.testType:
                    self.resetVars(self.TYPES[j], i + 1)
                    self.TYPE_FUNCS[j](i + 1)
                
    def deletePrevTests(self):
        targetFolders = ['src/test/Generated Tests/resources', 'src/test/Generated Tests/data']
        for target in targetFolders:
            self.clearFolder(target)
            for CurrentType in self.TYPES:
                os.makedirs(target + '/' + CurrentType)
        
    def clearFolder(self, folder):
        if not os.path.isdir(folder):
            os.makedirs(folder)
        for filename in os.listdir(folder):
            
            file_path = os.path.join(folder, filename)
            try:
                if os.path.isfile(file_path) or os.path.islink(file_path):
                    os.unlink(file_path)
                elif os.path.isdir(file_path):
                    shutil.rmtree(file_path)
            except Exception as e:
                print('Failed to delete %s. Reason: %s' % (file_path, e))
        
            
gt = GenerateTest()
gt.deletePrevTests()
gt.generateAllTests()   
    
