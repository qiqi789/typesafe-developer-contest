Biological Pathway Knowledge Mapping
==========

## Introduction

The purpose of this program was to extract knowledge from biological pathways of a given organism. A biological pathway is a graphical representation of a  network of gene products and chemical compounds that interacts to achieve a cellular function. The resulted collection of knowledge will be a valuable asset for constructing or analyzing biological pathways.

The pathways of an organism  were collected based on mappings of its genes against a pathway database, such as the [KEGG](http://www.genome.jp/kegg/kegg2.html). These pathways are also called reference pathways, and there maybe only a part of a reference pathway can be mapped by an organism's genes. An example can be referred to [Riboflavin metabolism - Reference pathway](http://www.genome.jp/kegg-bin/show_pathway?ko00740), and such a pathway can be downloaded as a XML formatted file.  The knowledge base we were going to collect was comprised by those mapped interactions in reference pathways.

The example attached with the program is given by yeast's genes and its mappings to KEGG Orthologies (KO), and corresponding reference pathways. The output of the running example will be a collection of all mapped gene-gene interaction or related chemical reaction information located in the reference pathways.


## Implementation

The source body contains three individual programs. The MapKnowledge.scala is the main program to carry out the computation. It uses Akka actors with three types of classes, Worker, Master and Listener. The Master will receive a calculation command and distributes work load evenly to Workers, and when Workers return results, it will combine them and send the result to the Listener. The Listener will then write the result into a file. 

The other two programs hold type definitions (as in KnowledgeBase.scala) and relevant utility functions (as in MapUtils.scala), respectively. 

The whole calculation is greatly benefited from the Akka. A list of yeast's genes as supplied in the given example will be partitioned and handed over to multiple Worker actors, and each of the genes will be matched against each of the reference pathways to find out mapped relations and reactions. The parallel computing model speeds up the whole process especially when there are many genes and reference pathways, which is a fairly common case.

Besides the programs, a running example is supplied by two text files (wholelist-and-ko.txt and wholelist-maps.txt), and a folder named kegg. The wholelist-and-ko.txt file contains a list of yeast's genes and its mapped KOs. The wholelist-maps.txt file contains a list of reference pathway names when mapping the genes against in the KEGG database. All of the corresponding reference pathways had been automatically downloaded and stored in the kegg folder.


## Instruction

To run the supplied example, type "sbt run" in a terminal. The result will be a text file named AllRelsOnMappedGenes_parallel.txt that contains all mapped relations and reactions of yeast genes existing in all the reference pathways.


## License

> Copyright [2012] [Qi Qi]
> 
>    Licensed under the Apache License, Version 2.0 (the "License");
>    you may not use this file except in compliance with the License.
>    You may obtain a copy of the License at
> 
>        http://www.apache.org/licenses/LICENSE-2.0
> 
>    Unless required by applicable law or agreed to in writing, software
>    distributed under the License is distributed on an "AS IS" BASIS,
>    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
>    See the License for the specific language governing permissions and
>    limitations under the License.
