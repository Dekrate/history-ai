2\. HistoriaAI – rozmowa z postaciami historycznymi + fakt-checker

Stack:



Backend: Spring Boot + Spring AI + Wikipedia REST API (darmowe, zero klucza)

Ollama: llama3.2:3b lub mistral (dobry w roli)

Frontend: React + Timeline.js + proste karty postaci



Funkcje edukacyjne:



Wybierasz postać (Kopernik, Maria Skłodowska-Curie, Piłsudski…).

Chat z nią – system prompt w Ollama + kontekst z Wikipedia.

Po rozmowie przycisk „Sprawdź fakty” – Java wyciąga z API i Ollama weryfikuje.

Tryb „debata” (uczeń vs postać) + zapis transkryptu do PDF.

Dla nauczycieli: generowanie gotowych scenariuszy lekcji.



Unikalność: Żaden projekt Java+Ollama tego nie robi. Jest tylko ogólny chat.

