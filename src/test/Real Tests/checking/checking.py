import os
import random
import shutil
from typing import Final
import copy

COMPONENTS: Final[list] = [
                 "4_1_BCS_MPW.dot", "4_2_BCS_APW.dot",
                 "4_3_BCS_FP.dot", "4_5_BCS_LED_FP.dot",
                 "4_6_BCS_CLS.dot", "4_7_BCS_EM.dot",
                 "4_9_BCS_LED_CLS.dot", "4_10_BCS_LED_MPW.dot",
                 "4_11_BCS_LED_APW.dot", "4_12_BCS_LED_EMT.dot",
                 "4_13_MCS_LED_EML.dot", "4_14_BCS_LED_EMB.dot",
                 "4_15_BCS_LED_EMR.dot", "4_16_BCS_LED_EMH.dot",
                 "4_17_BCS_LED_AS_Active.dot", "4_18_BCS_LED_AS_Alarm.dot",
                 "4_19_BCS_LED_AS_Alarm_D.dot", "4_20_MCS_LED_AS_IMA.dot",
                 "4_21_BCS_AS.dot", "BCS_PW_4.dot"
             ]

SYNCHS: Final[list] = [
    [
        "4_1_BCS_MPW.dot",
        "4_2_BCS_APW.dot",
        "4_3_BCS_FP.dot",
        "4_5_BCS_LED_FP.dot",
        "BCS_PW_4.dot"
    ],
    [
        "4_6_BCS_CLS.dot",
        "4_21_BCS_AS.dot"
    ],
    [
        "4_12_BCS_LED_EMT.dot",
       " 4_14_BCS_LED_EMB.dot"
    ],
    [
        "4_13_MCS_LED_EML.dot",
        "4_15_BCS_LED_EMR.dot"
    ]
]

TESTS_FOLDER = "src/test/Real Tests/resources/"

import os
        
def getActAndOut(line):
    labelVisited = False
    slashVisited = False
    doubleQuote = False
    
    action = ''
    out = ''
    
    for i in range(len(line)):
        if (not labelVisited) and line[i : (i+5)] == "label":
            labelVisited = True
            i = i + 5
        elif labelVisited and line[i] == '/':
            slashVisited = True
        elif ((labelVisited and (not slashVisited)) and (not doubleQuote)) and line[i] == '"':
            doubleQuote = True
        elif ((labelVisited and (not slashVisited)) and doubleQuote) and line[i] != '/':
            action += line[i]
        elif ((labelVisited and (not slashVisited)) and doubleQuote) and line[i] == '/':
            slashVisited = True
        elif ((labelVisited and slashVisited) and doubleQuote) and line[i] != '"':
            out += line[i]
        elif ((labelVisited and slashVisited) and doubleQuote) and line[i] == '"':
            action = action.strip()
            out = out.strip()
            return action, out

inputDir = "D:\Projects\SCL-Star\src/test\Real Tests\checking/resources/"

adjency = dict()
acts = dict()
for comp in COMPONENTS:
    with open(os.path.join(inputDir, comp), 'r') as file:
        acts[comp] = []
        adjency[comp] = []
        while True:
            line = file.readline()
            if not line:
                break
            line = line.strip()
            if len(line) == 0:
                continue
            
            # if inputDir == "Complete_FSM_files/":
            #     if line[0] != 's':
            #         continue;
                
            #     splittedLine = line.split()
            #     if len(splittedLine) != 6:
            #         continue;
                
            #     currentAct = decodeAct(splittedLine[3], '"')
            #     currentOut = decodeOut(splittedLine[5], '"')
                
            # elif inputDir == "experiment_models/":
            if not ("label" in line and "/" in line):
                continue
            
            currentAct, currentOut = getActAndOut(line)
            if currentAct not in acts[comp]:
                acts[comp].append(currentAct)
                for i in range(COMPONENTS.index(comp)):
                    if (currentAct in acts[COMPONENTS[i]]) and (comp not in adjency[COMPONENTS[i]]):
                        if not adjency[COMPONENTS[i]]:
                            adjency[COMPONENTS[i]] = [comp]
                        else:
                            adjency[COMPONENTS[i]].append(comp)

                        if not adjency[comp]:
                            adjency[comp] = [COMPONENTS[i]]
                        else:
                            adjency[comp].append(COMPONENTS[i])


for comp in COMPONENTS:
    print(comp + ":")
    print("\tacts: ")
    for act in acts[comp]:
        print("\t\t" + act)
    print("\tadjs: ")
    for adj in adjency[comp]:
        print("\t\t" + adj)


