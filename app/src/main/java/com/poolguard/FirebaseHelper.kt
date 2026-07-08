package com.poolguard

import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

data class ClienteFirebase(
    val nome: String = "",
    val endereco: String = "",
    val telefone: String = "",
    val dias: String = "",
    val frequencia: String = ""
)

class FirebaseHelper {

    private val database: DatabaseReference

    init {
        try {
            val firebase = Firebase.database("https://tomazela-piscinas-default-rtdb.firebaseio.com")
            database = firebase.reference
            Log.d("PoolGuard", "Firebase conectado com sucesso!")
        } catch (e: Exception) {
            Log.e("PoolGuard", "Erro Firebase: ${e.message}")
            throw e
        }
    }

    fun buscarClientes(callback: (List<ClienteFirebase>) -> Unit) {
        val clientesRef = database.child("clientes")
        
        clientesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val clientes = mutableListOf<ClienteFirebase>()
                for (child in snapshot.children) {
                    val nome = child.child("nome").getValue(String::class.java) ?: ""
                    val endereco = child.child("endereco").getValue(String::class.java) ?: ""
                    val telefone = child.child("telefone").getValue(String::class.java) ?: ""
                    val dias = child.child("dias").getValue(String::class.java) ?: ""
                    val frequencia = child.child("frequencia").getValue(String::class.java) ?: ""
                    
                    if (nome.isNotEmpty()) {
                        clientes.add(ClienteFirebase(nome, endereco, telefone, dias, frequencia))
                    }
                }
                Log.d("PoolGuard", "Clientes carregados: ${clientes.size}")
                callback(clientes)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PoolGuard", "Erro ao buscar: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun salvarCliente(cliente: ClienteFirebase) {
        val clientesRef = database.child("clientes")
        val key = clientesRef.push().key ?: return
        
        val map = mapOf(
            "nome" to cliente.nome,
            "endereco" to cliente.endereco,
            "telefone" to cliente.telefone,
            "dias" to cliente.dias,
            "frequencia" to cliente.frequencia
        )
        
        clientesRef.child(key).setValue(map)
            .addOnSuccessListener {
                Log.d("PoolGuard", "Cliente salvo: ${cliente.nome}")
            }
            .addOnFailureListener {
                Log.e("PoolGuard", "Erro ao salvar: ${it.message}")
            }
    }
}
