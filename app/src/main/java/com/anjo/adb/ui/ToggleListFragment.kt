package com.anjo.adb.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anjo.adb.R
import com.anjo.adb.core.ShellEngine
import com.anjo.adb.core.ToggleActionMapper
import com.anjo.adb.data.Categoria
import com.anjo.adb.data.SettingsRepository
import com.anjo.adb.data.ToggleRegistry

class ToggleListFragment : Fragment(R.layout.fragment_toggle_list) {

    companion object {
        private const val ARG_CATEGORIA = "categoria"
        fun novo(categoria: Categoria): ToggleListFragment {
            val f = ToggleListFragment()
            f.arguments = Bundle().apply { putString(ARG_CATEGORIA, categoria.name) }
            return f
        }
    }

    private lateinit var repo: SettingsRepository
    private lateinit var adapter: ToggleCardAdapter
    private val pendentes = linkedMapOf<String, Boolean>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = SettingsRepository(requireContext())
        val categoria = Categoria.valueOf(requireArguments().getString(ARG_CATEGORIA)!!)
        val itens = ToggleRegistry.porCategoria(categoria)

        adapter = ToggleCardAdapter(itens, repo) { item, ativo ->
            pendentes[item.id] = ativo
            atualizarContador()

            if (ativo && item.precisaShizuku && !ShellEngine.isReady()) {
                ShellEngine.requestPermission()
            }
            if (item.id == "foco_jogo" && ativo) pedirPermissoesDoFoco()
        }

        view.findViewById<RecyclerView>(R.id.recyclerToggles).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ToggleListFragment.adapter
        }

        val botaoAplicar = view.findViewById<View>(R.id.btnAplicar)
        botaoAplicar.setOnClickListener { aplicarPendentes() }
        com.anjo.adb.util.ThemeTint.aplicarEmBotaoPill(botaoAplicar)
        atualizarContador()
    }

    /** Foco no Jogo precisa de 2 permissões do Android que não dependem do Shizuku. */
    private fun pedirPermissoesDoFoco() {
        val nm = requireContext().getSystemService(android.app.NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Toast.makeText(requireContext(), "Ative o acesso ao Não Perturbe pro Anjo ADB", Toast.LENGTH_LONG).show()
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rm = requireContext().getSystemService(android.app.role.RoleManager::class.java)
            if (rm.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING)
            ) {
                startActivityForResult(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING), 991)
                Toast.makeText(requireContext(), "Aceite o Anjo ADB como app de triagem de chamadas pra bloquear ligação no jogo", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun atualizarContador() {
        val sub = view?.findViewById<TextView>(R.id.txtAplicarSub) ?: return
        sub.text = if (pendentes.isEmpty())
            "Toque pra aplicar tudo de uma vez"
        else
            "Toque pra aplicar tudo de uma vez • ${pendentes.size} pendente(s)"
    }

    private fun aplicarPendentes() {
        if (pendentes.isEmpty()) {
            Toast.makeText(requireContext(), "Nada pra aplicar ainda", Toast.LENGTH_SHORT).show()
            return
        }
        DisclaimerDialog.mostrarSeNecessario(requireContext()) {
            var erros = 0
            for ((id, ativo) in pendentes) {
                val erro = ToggleActionMapper.aplicar(requireContext(), id, ativo)
                if (erro != null) erros++
            }
            pendentes.clear()
            atualizarContador()
            Toast.makeText(
                requireContext(),
                if (erros == 0) "Aplicado com sucesso" else "Aplicado com $erros erro(s) — veja os avisos de Shizuku",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
