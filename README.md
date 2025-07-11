<h1 align="center">
  <div>
    <img width="80" src="https://raw.githubusercontent.com/itischrisd/itis-PJATK/main/logo.svg" alt="" />
  </div>
  RDL (Router Definition Language)
</h1>

Repository that contains final project implementation for AUG (Automata and Grammars) practical classes, taught by Łukasz Maśko during studies on [PJAIT](https://www.pja.edu.pl/en/).

Project's main goal is to implement a simple language that allows to define web application controllers and their routes. JFlex and CUP were used to create a parser and lexer for the language, which has some example inputs in resource files.

The following code is distributed under the [GPLv3](./LICENSE).

---

If you need some help, notice any bugs or come up with possible improvements, feel free to reach out to me and/or create a pull request. Keep in mind that some code may not be well optimized, as algorithm implamantation had to follow the variants presented during subject's lectures. Most tree strctures have objective implementation (excluding Binary Heap, which uses classic array) for the sake of more human readable logic. In such cases traversing and node lookup time complexity might not follow the real implemantation, but rather skip over it and assume best-case variants.
