import string
import random
from typing import Final
from decimal import Decimal

class ComponentGenerator:
    def __init__(self, synchActions, unsynchActs, numOfStates):
        self.synchActions: Final[list] = synchActions
        self.unsynchActs: Final[list] = unsynchActs
        self.numOfStates: Final[int] = numOfStates
        self.IsMinimum: Final[int] = 1
        self.IsNotMin: Final[int] = 0
        self.transitions = [dict() for i in range(numOfStates)]
        self.graph = ''
        self.causeOfNotMin = ''
        self.isReachable = [False] * numOfStates

    def expandBfsQueue(self, actions, u, queue):
        for act in actions:
            v = self.transitions[u][act][0]
            if(not self.isReachable[v]):
                self.isReachable[v] = True
                queue.append(v)

    def bfs(self, startState):
        queue = []

        self.isReachable[startState] = True
        queue.append(startState)

        while(len(queue) != 0):
            u = queue.pop()
            
            self.expandBfsQueue(self.synchActions, u, queue)
            self.expandBfsQueue(self.unsynchActs, u, queue)

            
    def refactorGraph(self):
        shouldChangeState = random.randint(0, self.numOfStates - 1)
        
        destState = random.randint(0, self.numOfStates - 2)
        if(destState >= shouldChangeState):
            destState += 1
            
        preOut = self.transitions[shouldChangeState][self.causeOfNotMin][1]
        self.transitions[shouldChangeState][self.causeOfNotMin] = (destState, preOut)
    
    def didActJustLoop(self, act):
        for state in range(self.numOfStates):
            if(self.transitions[state][act][0] != state):
                return(False)
        return(True)
    
    def checkMinForActs(self, acts):
        for act in acts:
            if(self.didActJustLoop(act)):
                self.causeOfNotMin = act
                return(False)
        return(True)
            
    def isEveryActEffective(self):
        if(not self.checkMinForActs(self.unsynchActs)):
            return(False)
        if(not self.checkMinForActs(self.synchActions)):
            return(False)
        
        return(True)
    
    
    def isEveryStateReachable(self):
        self.isReachable = [False] * self.numOfStates
        self.bfs(0)

        if(False in self.isReachable):
            return(False)
        else:
            return(True)
        
    def findEnoughReachableState(self, sourceState, acts, stateId):
        for act in acts:
            sinkState = self.transitions[sourceState][act][0]
            self.transitions[sourceState][act][0] = stateId
            numOfPrevReachables = self.isReachable.count(True)
            self.isReachable = [False] * self.numOfStates
            self.bfs(0)
            if(self.isReachable.count(True) > numOfPrevReachables):
                return(sinkState)
            self.transitions[sourceState][act][0] = sinkState
            self.bfs(0)
        return(-1)

        
    def makeReachable(self, stateID):
        for sourceState in range(self.numOfStates):
            if(not self.isReachable[sourceState]):
                continue
            sinkState = self.findEnoughReachableState(sourceState, self.synchActions, stateID)
            if(sinkState == -1):
                sinkState = self.findEnoughReachableState(sourceState, self.unsynchActs, stateID)
            if(sinkState != -1):
                break
        assert sinkState != -1
            
                
    def makeStatesReachable(self):
        for stateId in range(self.numOfStates):
            if(not self.isReachable[stateId]):
                self.makeReachable(stateId)
                
    def areFunctionallySameStates(self, state1, state2, determinantAct):
        visitedStates = [False] * ((self.numOfStates-1)*10 + self.numOfStates)
        currentS1 = state1
        currentS2 = state2
        currentStates = (currentS1 * 10) + currentS2
        
        while (not visitedStates[currentStates]) and (currentS1 != currentS2):
            visitedStates[currentStates] = True
            if self.transitions[currentS1][determinantAct][1] !=\
                self.transitions[currentS2][determinantAct][1]:
                return False
            currentS1 = self.transitions[currentS1][determinantAct][0]
            currentS2 = self.transitions[currentS2][determinantAct][0]
            currentStates = (currentS1 * 10) + currentS2
        return True
                
    def doesAct1EffectOnAct2(self, act1, act2):
        for state in range(self.numOfStates):
            if not self.areFunctionallySameStates(state, self.transitions[state][act1][0], act2):
                return True
        return False
                
    def areIndependentActs(self, act1, act2):
        if self.doesAct1EffectOnAct2(act1=act1, act2=act2) or\
            self.doesAct1EffectOnAct2(act1=act2, act2=act1):
            return True
        return False
                
    def isGraphMinimal(self):
        allActs = self.synchActions + self.unsynchActs
        isEffective = [False] * len(allActs)
        effectives = [-1] * len(allActs)
        
        isEffective[0] = True
        effectives[0] = 0
        firstEmptyIndex = 1
        
        for effectiveActIndex in effectives:
            if effectiveActIndex == -1:
                return False
            for uncheckedActIndex in range(len(allActs)):
                if isEffective[uncheckedActIndex]:
                    continue
                if self.areIndependentActs(allActs[effectiveActIndex], allActs[uncheckedActIndex]):
                    if firstEmptyIndex == (len(allActs) - 1):
                        return True
                    effectives[firstEmptyIndex] = uncheckedActIndex
                    firstEmptyIndex += 1
                    isEffective[uncheckedActIndex] = True
        return True
                       
    def findEqualStates(self, stateNum, action):
        equals = set()
        for currState in range(self.numOfStates):
            if self.areFunctionallySameStates(stateNum, currState, action):
                equals.add(currState)
        return equals

    def generateAll(self, isForTransitions):
        self.equalStates = [{i for i in range(self.numOfStates)} for i in range(self.numOfStates)]

        for synchNum in range(len(self.synchActions)):
            for stateNum in range(self.numOfStates):
                if(isForTransitions):
                    self.generateTransition(stateNum, self.synchActions[synchNum], [i for i in range(self.numOfStates)]) 
                else:
                    self.generateLine(stateNum, self.synchActions[synchNum])

            for stateNum in range(self.numOfStates):
                self.equalStates[stateNum] &= self.findEqualStates(stateNum, self.synchActions[synchNum])

        for unsynchNum in range(len(self.unsynchActs)):
            for stateNum in range(self.numOfStates):
                if(isForTransitions):
                    self.generateTransition(stateNum, self.unsynchActs[unsynchNum], list(self.equalStates[stateNum]))
                else:
                    self.generateLine(stateNum, self.unsynchActs[unsynchNum])

        

    def generate(self):
        while True:
            self.generateAll(isForTransitions = True)

            while all(len(self.equalStates[i]) == 1 for i in range(self.numOfStates)):
                self.generateAll(isForTransitions = True)
            
            # while (not self.isEveryActEffective()):       
            #     self.refactorGraph()
            
            if (self.isEveryStateReachable()) and \
                (self.isGraphMinimal() and self.isEveryActEffective()):
                break
        self.generateGraphStr()
        return(self.graph)

    def generateTransition(self, stateNum, action, possibleDests):
        self.transitions[stateNum][action] = [random.choice(possibleDests), random.randint(0, 1)]
            
    def generateLine(self, stateNum, action):
        self.graph += 's' + str(stateNum) + ' -> '
        self.graph += 's' + str(self.transitions[stateNum][action][0])
        self.graph += ' [label="' + action + '  /  ' + str(self.transitions[stateNum][action][1]) + '"];\n'
        
    def generateGraphStr(self):
        self.generateAll(isForTransitions = False)

# cg = ComponentGenerator(['N'], [1], ['Q'], 2)
# cg.transitions[0]['N'] = [0, 1]

# cg.transitions[0]['Q'] = [1, 1]

# cg.transitions[1]['N'] = [0, 1]

# cg.transitions[1]['Q'] = [0, 1]

# temp = cg.isGraphMinimal()
# print(temp)



            