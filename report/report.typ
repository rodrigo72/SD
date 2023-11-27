#import "template.typ": *
#import "bytefield.typ": *
#show: LNCS-paper.with(
  title: "Cloud Computing",
  subtitle: "Trabalho Prático - Sistemas Distribuídos",
  university: "Universidade do Minho, Departamento de Informática",
  email_type: "alunos.uminho.pt",
  authors: (
    (
      name: "Rodrigo Monteiro",
      number: "a100706",
    ),
    (
      name: "Diogo Abreu",
      number: "a100646",
    ),
    (
      name: "Filipa Pinto",
      number: "a96862",
    ),
    (
      name: "Flávio Silva",
      number: "a97352"
    )
  ),
  bibliography-file: "refs.bib",
  bibliography-full: true
)

#set list(marker: [--], indent: 7pt)

= Introdução

Neste projeto, implementamos um serviço de #emph("cloud computing") com funcionalidade #emph("Function-as-a-Service:") clientes enviam código de tarefas de computação para um servidor principal, que faz uma distribuição para outros servidores, especializados na execução de tarefas, de acordo com a memória necessária para execução, e de acordo com as configurações de memória desses servidores -- a que chamamos #emph("workers").

Para isso, utilizamos a linguagem Java, #emph("threads") e #emph("sockets") TCP, respeitando os seguintes requisitos: uma única conexão entre cada duas máquinas envolvidas; um protocolo de comunicação em formato binário; e cada #emph("thread") do servidor associada a apenas um #emph("socket"). E tendo como objetivos minimizar o número #emph("threads") acordadas, diminuir a contenção e assegurar que não ocorre #emph("starvation").

Implementamos, também, interfaces simples para o cliente e para os #emph("workers") com o padrão #emph("Model-View-Controller"). 

= Protocolo

Todas as mensagens protocolares definidas começam com dois campos: um identificador, ID, e o tipo de mensagem. Sendo o ID do tipo primitivo `long`, e o tipo da mensagem identificado através de um `int`.
#[ #set text(size: 7pt)
  #bytefield(
    bits: 96,
    bitheader: (0, 64, 95),
    bits(64)[ID], 
    bits(32)[Type]
  )
]

Assim, todas as classes que representam as mensagens protocolares são subclasses de uma classe `Packet`, que possui os atributos `id`, e `PacketType` -- uma interface que o tipo de mensagem tem de implementar.

#[ #set text(size: 8pt)
```java
public abstract class Packet {
    private final long id;
    private final PacketType type;
    // ...
public interface PacketType {
    int getValue();
    static void serialize(PacketType type, DataOutputStream out) throws IOException 
    { out.writeInt(type.getValue()); }
    static <T extends Enum<T> & PacketType> T 
    (DataInputStream in, Class<T> enumType) throws IOException, IllegalArgumentException 
    { /* ... */ }
}
```
]

Decidimos agrupar os métodos de serialização e deserialização em classes próprias.
Caso os métodos fossem implementados nas subclasses de `Packet`, estes seriam úteis se fossem `static`, o que não resultaria com herança, e levaria a repetição de código.

#[
  #set text(size: 8pt)
```java
public class ClientPacketDeserializer implements Deserializer {
    public Packet deserialize(DataInputStream in) throws IOException { ... }
}
```
]

== Client packets

Os clientes enviam pacotes do tipo #emph("Registration, Login, Job,") e #emph("Status"). Podem fazer um registo, fornecendo um nome único e uma palavra-passe, ou podem efetuar login. Depois disso, podem enviar pedidos de execução de tarefas, verificar pedidos enviados ou recebidos, ou fazer logout.

#[
  #set text(size:9pt)
- Registration e Login
  - #emph("Name:") Nome único do cliente. (Tamanho variável).
  - #emph("Password:") Palavra-passe relativa ao registo. (Tamanho variável).
- Logout
- Job request
  - #emph("Required memory:") Memória necessária para executar o código da tarefa. (8 bytes).
  - #emph("Data length:") Comprimento do array de bytes. (8 bytes).
  - #emph("Data:") Array de bytes que representa o código da tarefa. (Tamanho variável).
- Status request
]
== Server packets

O servidor principal é responsável por receber e gerir os pedidos de registo, login e logout, tal como gerir uma fila de tarefas, e a distribuição destas através das conexões com os #emph("workers").

#[
  #set text(size:9pt)
- Information
  - #emph("Memory limit:") Limite máximo de memória que uma tarefa pode ter. (8 bytes).
  - #emph("Total memory:") Memória total, isto é, a soma da memória dos servidores conectados. (8 bytes).
  - #emph("Used memory:") Memória a ser utilizada pelos servidores. (8 bytes).
  - #emph("Queue size:") Tamanho da fila de tarefas. (4 bytes)
  - #emph("Nº connections:") Número de clientes conectados. (4 bytes).
  - #emph("Nº workers") Número de servidores conectados. (4 bytes).
  - #emph("Nº workers waiting:") Número de servidores à espera de tarefas. (4 bytes).
- Job request (enviado para os #emph("workers"))
  - #emph("Client name:") Nome do cliente que enviou o pedido de execução da tarefa. (Tamanho variável).
  - #emph("Required memory:") Memória necessária para executar o código. (8 bytes)
  - #emph("Data length:") Comprimento do array de bytes. (8 bytes).
  - #emph("Data:") Array de bytes que representa o código da tarefa. 
- Job result
  - #emph("Result status:") Identifica se foi possível executar a tarefa. (4 bytes)
  - #emph("Error message:") Caso não tenha sido possível executar a tarefa, é enviada a mensagem de erro produzida. (Tamanho variável)
  - #emph("Data length:") Tamanho do output produzido. (8 bytes).
  - #emph("Data:") Array de bytes do output. (Tamanho variável).
- Status
  - #emph("Status:") Identificador do estado em relação a um pedido do cliente. (4 bytes) 
]

== Worker packets

Um worker envia inicialmente um pedido de #emph("Connection"), e é responsável por receber pedidos de execução de tarefas, executar as tarefas e enviar os resultados para o servidor principal. Por fim, envia um pedido de #emph("Disconnection"), para informar o servidor principal que deixa de estar disponível.

#[
  #set text(size:9pt)
- Connection
  - #emph("Memory:") Memória disponível do servidor para a execução de tarefas.
- Disconnection
- Job Result
  - #emph("Client name:") Nome do cliente que pediu a execução da tarefa. (Tamanho variável).
  - #emph("Result status:") Identifica se foi possível executar a tarefa. (4 bytes)
  - #emph("Error message:") Caso não tenha sido possível executar a tarefa, é enviada a mensagem de erro produzida. (Tamanho variável)
  - #emph("Data length:") Tamanho do output produzido. (8 bytes).
  - #emph("Data:") Array de bytes do output. (Tamanho variável).
]

= Implementação

== Client

A classe `Client` implementa a seguinte interface, #emph("ClientAPI"):

```java
public interface ClientAPI {
    void createRegistration(String name, String password);
    long sendRegistration() throws IOException;
    long sendLogin() throws IOException;
    long sendLogout() throws IOException;
    long sendJob(int requiredMemory, byte[] job) throws IOException;
    long sendGetInfo() throws IOException;
    Packet receive(long id) throws IOException, InterruptedException;
    Packet fastReceive(long id) throws IOException, InterruptedException;
    List<Packet> getJobRequests();
    List<Packet> getJobResults();
    void exit() throws IOException;
}
```

Para além disso, possui os atributos `ClientPacketSerializer` que utiliza para serializar e enviar mensagens para o servidor, `ServerPacketDeserializer` que utiliza para deserializar as mensagens que recebe do servidor, `Demultiplixer` que utiliza para receber mensagens do servidor, organizando-as por ID, permitindo que se espere por uma ou mais mensagens com um ID específico, entre outros.

Na implementação, não foi necessário receber mais do que uma mensagem com o mesmo ID. Apesar disso, optamos por utilizar uma `ConditionQueue<Packet>` para cada ID como uma medida proativa para garantir flexibilidade.

== Server

== Worker

= Funcionamento

= Conclusões e trabalho futuro