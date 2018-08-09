lodreclib - Linked Open Data Recommender Systems Library
=====================

lodreclib is a Java library to build recommendation engines fed by Linked (Open) Data datasets. It exposes methods to extract information from DBpedia or other RDF knowledge graphs via SPARQL queries and then to use it to train your LOD-based recommender system. lodreclib comes with the implementation of SPrank and two graph kernel methods as recommendation algorithms (see References). 

How to use
------------
Before running lodreclib, the `config.properties` file must be properly set. For more details about that configuration, see our [documentation](https://github.com/sisinflab/lodreclib/wiki).
The `config.properties` file provided in this repositoriy is set as an example using the Movielens dataset, whose files are in the movielens folder.

Using Maven, it is possible to build a runnable jar with the command 
~~~
mvn package
~~~
and is executable by the command
~~~
java -jar lodreclib-0.0.1-SNAPSHOT.jar 
~~~


References
------------
If you publish research that uses lodreclib, please cite it as
~~~
@Article{DOTD16, 
  author = {{Di Noia}, Tommaso and Ostuni, Vito Claudio and Tomeo, Paolo and {Di Sciascio}, Eugenio},
 title = {SPrank: Semantic Path-Based Ranking for Top-N Recommendations Using Linked Open Data},
 journal = {ACM Trans. Intell. Syst. Technol.},
 issue_date = {October 2016},
 volume = {8},
 number = {1},
 month = sep,
 year = {2016},
 issn = {2157-6904},
 pages = {9:1--9:34},
 articleno = {9},
 numpages = {34},
 doi = {10.1145/2899005},
 publisher = {ACM},
 keywords = {DBpedia, Learning to rank, hybrid recommender systems},
} 
~~~
~~~
@Article{OODSD16, author = {
  author = {Oramas, Sergio and Ostuni, Vito Claudio and {Di Noia}, Tommaso and Serra, Xavier and {Di Sciascio}, Eugenio},
 title = {Sound and Music Recommendation with Knowledge Graphs},
 journal = {ACM Trans. Intell. Syst. Technol.},
 issue_date = {January 2017},
 volume = {8},
 number = {2},
 month = oct,
 year = {2016},
 issn = {2157-6904},
 pages = {21:1--21:21},
 articleno = {21},
 numpages = {21},
 doi = {10.1145/2926718},
 publisher = {ACM},
 keywords = {Knowledge graphs, diversity, entity linking, music, novelty, recommender systems},
} 
~~~
Credits
------------
This library was originally developed by Vito Mastromarino for his Master thesis at Polytechnic University of Bari under the supervision of Tommaso Di Noia, Vito Claudio Ostuni and Paolo Tomeo.

Contacts
------------
Tommaso Di Noia, tommaso [dot] dinoia [at] poliba [dot] it  
Paolo Tomeo, paolo [dot] tomeo [at] poliba [dot] it  
Vito Mastromarino, zlatanito88 [at] gmail [dot] com  
Vito Claudio Ostuni, vitoclaudio [dot] ostuni [at] poliba [dot] it  

