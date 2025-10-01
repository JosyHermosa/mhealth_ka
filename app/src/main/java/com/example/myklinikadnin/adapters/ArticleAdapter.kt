package com.example.myklinikadnin.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myklinikadnin.R
import com.example.myklinikadnin.data.Article

class ArticleAdapter(private val articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.articleTitle)
        val desc: TextView = view.findViewById(R.id.articleDesc)
        val image: ImageView = view.findViewById(R.id.articleImage)
        val readMore: Button = view.findViewById(R.id.btnReadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.title.text = article.title
        holder.desc.text = article.description

        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(article.imageUrl)
            .into(holder.image)

        holder.readMore.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = articles.size
}
