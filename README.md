# Graph Instance Classifier

A library to solve the following graph based problem:

Given a knowledge graph _K_ and two sets of instance graphs _An_ and _Bn_ that contain links to _K_, 
determine characteristics of graphs in _Bn_ that differentiate them from _An_.

## Graph Instance Classification Techniques

Multiple techniques will be used to determine the differentiating characteristics of _Bn_.
The techniques will be applied in order, starting with the simplest, until sufficient characteristics are found.

### Differentiating links to K
_Status: Working_

### Differentiating sets of links to K
_Status: In progress_

#### Steps
1. Collect and count unique sets of links from _An_ and _Bn_
2. For each set calculate coverage over group Bn and accuracy
3. Attempt to increase coverage of patterns by generalising cases but only where accuracy is not reduced
   1. Optional links
   2. Generalise link using parent or ancestor from _K_

### Differentiating series of links to K
_Status: Planned_

### Use attributes of link concepts to find correlation?
_Status: Planned_
