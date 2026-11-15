package com.zewmod.engine.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zewmod.engine.R
import com.zewmod.engine.ZewModApp
import com.zewmod.engine.bridge.RelayBridge
import com.zewmod.engine.databinding.FragmentServerBinding
import com.zewmod.engine.model.ServerInfo
import com.zewmod.engine.ui.adapter.ServerAdapter
import kotlinx.coroutines.launch

class ServerFragment : Fragment(R.layout.fragment_server) {

    private var _binding: FragmentServerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServerAdapter
    private var servers = mutableListOf<ServerInfo>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupRefresh()
        loadServers()
    }

    private fun setupRecycler() {
        val prefs = (requireActivity().application as ZewModApp).prefs
        adapter = ServerAdapter(
            selectedName = prefs.selectedServer,
            onSelect = { server ->
                prefs.selectedServer = server.name
                adapter.setSelected(server.name)
                // Update engine config
                (requireActivity().application as ZewModApp).updateConfig(
                    (requireActivity().application as ZewModApp).config.copy(
                        name = server.name,
                        ip = server.ip,
                        port = server.port,
                        key = server.key,
                        features = server.features
                    )
                )
            }
        )
        binding.rvServers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvServers.adapter = adapter
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener { pingServers() }
        binding.btnRefreshPing.setOnClickListener { pingServers() }
    }

    private fun loadServers() {
        servers = RelayBridge.defaultServers().toMutableList()
        adapter.submitList(servers.toList())
        pingServers()
    }

    private fun pingServers() {
        binding.swipeRefresh.isRefreshing = true
        binding.tvServerStatus.text = "Đang kiểm tra ping..."
        lifecycleScope.launch {
            val updated = RelayBridge.pingAll(servers)
            servers.clear()
            servers.addAll(updated)
            adapter.submitList(servers.toList())

            val onlineCount = servers.count { it.ping >= 0 }
            binding.tvServerStatus.text = "DANH SÁCH SERVER • $onlineCount/${servers.size} online"
            binding.swipeRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
