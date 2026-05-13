package com.fitme.app.data.repository

import com.fitme.app.data.model.DressAnnotation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AnnotationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("dress_annotations")

    suspend fun saveAnnotation(annotation: DressAnnotation): Result<DressAnnotation> {
        return try {
            val id = annotation.annotationId.ifEmpty { UUID.randomUUID().toString() }
            val finalAnnotation = annotation.copy(annotationId = id)
            collection.document(id).set(finalAnnotation).await()
            Result.success(finalAnnotation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToAnnotations(
        customerId: String,
        onUpdate: (List<DressAnnotation>) -> Unit
    ) {
        collection
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val annotations = it.toObjects(DressAnnotation::class.java)
                    onUpdate(annotations)
                }
            }
    }

    suspend fun deleteAnnotation(annotationId: String): Result<Unit> {
        return try {
            collection.document(annotationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToAnnotationsByOrder(
        orderId: String,
        onUpdate: (List<DressAnnotation>) -> Unit
    ) {
        collection
            .whereEqualTo("orderId", orderId)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val annotations = it.toObjects(DressAnnotation::class.java)
                    onUpdate(annotations)
                }
            }
    }
    suspend fun deleteAllUserAnnotations(customerId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            val snapshots = collection.whereEqualTo("customerId", customerId).get().await()
            for (doc in snapshots.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
