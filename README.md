# Graph Instance Classifier

A library to solve the following graph based problem:

Given a knowledge graph _K_ and two sets of instance graphs _An_ and _Bn_ that contain links to _K_, 
determine characteristics of graphs in _Bn_ that differentiate them from _An_.

## Graph Instance Classification Techniques

Multiple techniques will be used to determine the differentiating characteristics of _Bn_.
The techniques will be applied in order, starting with the simplest, until sufficient characteristics are found.

### Differentiating links to K
_Status: Planned_

### Differentiating sets of links to K
_Status: In progress, Steps 1, 2 & 3.1 working_

#### Steps
1. Collect and count unique sets, or patterns, of links from _An_ and _Bn_
2. For each pattern calculate coverage over group Bn and accuracy
3. Attempt to increase coverage of patterns by generalising cases but only where accuracy is not reduced
   1. Optional links
   2. Generalise link using parent or ancestor from _K_

### Differentiating series of links to K
_Status: Planned_

### Use attributes of link concepts to find correlation?
_Status: Planned_

## How does this relate to data analytics tool?
- it could start by performing encounter frequency diff
- then investigate frequency with pairs of encounters? Then triples?
- The sets of encounters could consider order.
- Good to build this using raw objects first before putting in Elastic?

In Raw folder - input to HDAD - for each identified concept from NLP - which patient -
- over six months ago
- current six
