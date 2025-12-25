
이 프로젝트의 목적은 RAG 를 샘플로 구현해 가능성을 보기 위함이다. 
해당 구현의 내용을 크게 벗어나지 않도록 새로운 기능을 넣지는 않고 구조를 정리하는 리팩토링이다. 

멀티 모듈 앱으로 리팩토링을 하려고 한다.
구성 모듈은 
(network)
(vectorStore)
(splitter)
(llm)
(embedding) 
으로 구성한다. 

각 모듈은 library/network, library/vectorStore, library/splitter, library/llm, Libarary/embedding 으로 만든다.
DI 는 Koin 을 사용하게 한다. 

## netowork 모듈
HttpClientFactory OpenAiApiClient, PineconeApiClient
등을 공통화 해소 network 모듈을 생성한다. 

OpenAiApiService, PineconeService 를 해당 url 을 넣고 build 해서 바로 사용할 수 있도록 
api 를 쉽게 찍어낼 수 있도록 하기 위한 목적이다 

지금 구현된 코드 이상의 구현은 필요 없고 지금 구현된 수준을 바탕으로 공통화 모듈이 필요하다. 

## vectorStore 모듈 
faiss 와 pinecone 모두를 포함하는 모듈이다. 
faiss 를 위한 cpp (ndk) 빌드 환경 jni 환경 interface code 를 모두 여기에 포함 시키고 domain layer 에서 필요한 interface 르ㅡㄹ 제공한다. 

pinecone 은 api 가 필요하고 해당 api 는 network module 을 이용해 service 를 빌드한다. 

### llm 모듈 
openAi 와 claude 모듈을 구성하고 역시 network module 을 통해 api 로 구현해줘 

### embedding 모듈 
openAi embedding, mediaPipeEmbedding 으로 구성하고 
역시 네트웤 모듈로 openAi embedding api 를 service interface 로 빌드하게 한다. 


### 기타 공통사항 
필요에 따라 vector store 를 추가하거나 embedding 방식 모델을 추가하거나 할 수 있다. 
llm 모듈 역시 필요에 따라 local llm 을 붙일 수 있으니 그 구조에 맞는 interface 를 정의 한다. 

package 구조는 
data/datastore 
data/model 
data/repositoryImpl 

domain/repository
domain/model 등 클린 아키텍처에 맞제 구성한다. 

usecase 들 - PineConeRepository - PineconeDataSource - pineconeApiService
usecase 들 - FaissRepository - FaissDataSource - Fiass ndk jni 
모두 vector store interface 로 외부랑 연동하고 di 할때 필요한 di 를 찾아서 연동하게 
