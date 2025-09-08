package bono.poc.pact.provider

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PactProviderKotlinSpringApplication

fun main(args: Array<String>) {
	runApplication<PactProviderKotlinSpringApplication>(*args)
}
