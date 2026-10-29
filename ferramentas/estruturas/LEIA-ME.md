# Geradores das estruturas grandes (3.1)

Os moldes `.nbt` e as tabelas de saque das estruturas grandes do datapack são feitos por código, não à mão.

- `GerarEstruturas.java` desenha as 6 estruturas (Árvore-Mundo, Colosso Caído, Cidade Anã, Ossada do Dragão, Coliseu, Castelo do Rei Caído) e grava os moldes; também salva uma prévia isométrica de cada uma.
- `GerarSaque.java` grava as tabelas de saque (baús, escavação, cofre e equipamento do Coliseu) e os spawners de desafio do Coliseu.
- `Molde.java` (o molde e o gravador NBT), `Snbt.java` (NBT dos blocos), `Previa.java` (prévia) e `NbtLer.java` (mostra um .nbt como texto).

Com Java 25, nesta pasta:

```
javac -d build *.java
java -cp build GerarEstruturas ../../src/main/resources/datapack/data/rpgatributos/structure previa
java -cp build GerarSaque ../../src/main/resources/datapack/data/rpgatributos
```

Cuidados:
- O ponto importante de cada estrutura (câmara do chefe, coração, salão, arena, trono) fica no meio do molde: o jogo gira a estrutura ao acaso e o plugin só conhece a caixa dela (`estruturas/Gigantes.java`).
- Se mudar o tamanho de um molde ou quanto ele começa abaixo do chão (`start_height` em `worldgen/structure`), mude também `Gigantes.tamanho` e `Gigantes.superficie`.
- Na 26.3 a paleta do molde usa `id` e `properties` (não `Name`/`Properties`); com a chave errada o jogo carrega o molde mas não põe nenhum bloco.
- Mudou o datapack: o plugin troca a cópia no mundo sozinho, mas o servidor precisa reiniciar mais uma vez.
