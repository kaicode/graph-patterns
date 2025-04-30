# Graph Instance Classifier

## Overview
A library to solve the following graph based problem:

Given a knowledge graph _K_ and two sets of instance graphs _An_ and _Bn_ that contain links to _K_, 
determine characteristics of graphs in _Bn_ that differentiate them from _An_.

## Use Case
The primary use case for this work is to prepare healthcare data for machine learning. 
When healthcare data is coded using a large terminology like SNOMED CT using the codes directly, without the semantics of the ontology, is likely to 
result poor pattern recognition and possible overfitting.

### Objective
To identify other phenotypes strongly associated with a high risk condition, for example diabetic foot, in patients with diabetes, 
leveraging the SNOMED CT ontology to guide phenotypic clustering and analysis.

### Inputs
1. **Cohort Data**: Electronic healthcare records (EHRs) for a cohort of patients with diabetes who are vulnerable to developing diabetic foot, where some have already developed it. 
2. **Selection Criteria for Diabetic Foot Codes**: List of top level SNOMED CT codes that can be used to identify patients within the cohort who already have diabetic foot. 

### Process
#### Step 1: Cohort Segmentation
**Define Group A**: Patients with diabetes who do not yet have diabetic foot (pre-diabetic foot group).
**Define Group B**: Patients with diabetes who already have diabetic foot (diabetic foot group).
**Exclusion of diabetic foot codes**: In this step, use diabetic foot-related SNOMED CT codes to divide patients into the two groups but exclude these codes from further analysis to prevent bias.

#### Step 2: Phenotype Identification
- Extract all other clinical phenotypes (SNOMED CT codes) from the records of patients in both groups.
  - Examples: Cardiovascular conditions, neuropathy, infections, skin changes.

TODO: Complete description - https://chatgpt.com/share/673e5f5d-54ec-8001-8761-1823274771f3

// This library uses the description logic subclass semantics of SNOMED CT to summarise and cluster differences between two patient groups. 
