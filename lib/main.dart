import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

const Color _bg = Color(0xFF0B1018);
const Color _panel = Color(0xFF111A26);
const Color _panel2 = Color(0xFF172334);
const Color _line = Color(0xFF263449);
const Color _blue = Color(0xFF69B7FF);
const Color _text = Color(0xFFE5EDF7);
const Color _muted = Color(0xFF91A2B8);
const MethodChannel _builder = MethodChannel('local_aide/builder');

void main() => runApp(const LocalAideApp());

class LocalAideApp extends StatelessWidget {
  const LocalAideApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Local AIDE',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          brightness: Brightness.dark,
          scaffoldBackgroundColor: _bg,
          colorScheme: const ColorScheme.dark(primary: _blue, surface: _panel),
          useMaterial3: true,
        ),
        home: const StudioPage(),
      );
}

class StudioPage extends StatefulWidget {
  const StudioPage({super.key});

  @override
  State<StudioPage> createState() => _StudioPageState();
}

class _StudioPageState extends State<StudioPage> {
  static const String _root = '/storage/emulated/0/LocalAIDE';
  static const String _projectsRoot = '$_root/projects';
  static const String _logsRoot = '$_root/logs';
  static const String _buildsRoot = '$_root/builds';

  final List<Directory> _projects = [];
  Directory? _project;
  TextEditingController _editor = TextEditingController();
  String _activePath = 'lib/main.dart';
  String _status = 'Ready';
  String _log = 'Create a Flutter project or open an existing one.\n\nFirst tap Install Local Toolchain. It downloads the build tools inside this app; no separate terminal app is used.';
  bool _busy = false;
  bool _storageReady = false;

  String get _projectName => _project?.path.split('/').last ?? '';
  String get _activeFilePath => _project == null ? '' : '${_project!.path}/$_activePath';

  @override
  void initState() {
    super.initState();
    _checkStorageStatus();
  }

  Future<void> _checkStorageStatus() async {
    if (!Platform.isAndroid) {
      if (mounted) setState(() => _status = 'Android device required for local builds');
      return;
    }
    final status = await Permission.manageExternalStorage.status;
    if (!mounted) return;
    setState(() {
      _storageReady = status.isGranted;
      _status = status.isGranted ? 'Local workspace ready' : 'Grant storage access to begin';
    });
    if (status.isGranted) await _refreshProjects();
  }

  Future<bool> _ensureStorage() async {
    if (!Platform.isAndroid) {
      _notice('The embedded local builder is intended for an Android device.');
      return false;
    }
    var status = await Permission.manageExternalStorage.status;
    if (!status.isGranted) status = await Permission.manageExternalStorage.request();
    if (!status.isGranted) {
      _notice('Allow Local AIDE to manage files so it can access project folders and APK output.');
      await openAppSettings();
      return false;
    }
    if (mounted) setState(() => _storageReady = true);
    for (final p in [_projectsRoot, _logsRoot, _buildsRoot]) {
      await Directory(p).create(recursive: true);
    }
    return true;
  }

  Future<void> _refreshProjects() async {
    if (!await _ensureStorage()) return;
    final root = Directory(_projectsRoot);
    final found = <Directory>[];
    if (await root.exists()) {
      await for (final entity in root.list(followLinks: false)) {
        if (entity is Directory &&
            await File('${entity.path}/pubspec.yaml').exists() &&
            await Directory('${entity.path}/android').exists()) {
          found.add(entity);
        }
      }
    }
    found.sort((a, b) => a.path.toLowerCase().compareTo(b.path.toLowerCase()));
    if (!mounted) return;
    setState(() {
      _projects
        ..clear()
        ..addAll(found);
      _status = 'Found ${found.length} Flutter project(s)';
    });
  }

  Future<void> _createProject() async {
    final nameController = TextEditingController(text: 'my_flutter_app');
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: _panel,
        title: const Text('New Flutter project'),
        content: TextField(
          controller: nameController,
          autofocus: true,
          textCapitalization: TextCapitalization.none,
          decoration: const InputDecoration(
            labelText: 'Project name',
            helperText: 'Lowercase letters, numbers and underscores',
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, nameController.text.trim()), child: const Text('Create')),
        ],
      ),
    );
    nameController.dispose();
    if (name == null) return;
    final clean = name.toLowerCase().replaceAll(RegExp(r'[^a-z0-9_]'), '_');
    if (clean.isEmpty || RegExp(r'^[0-9]').hasMatch(clean)) {
      _notice('Enter a valid name, such as my_flutter_app.');
      return;
    }
    if (!await _ensureStorage()) return;
    if (await Directory('$_projectsRoot/$clean').exists()) {
      _notice('A project with that name already exists.');
      return;
    }
    await _runBuilder(action: 'createProject', name: clean);
    final created = Directory('$_projectsRoot/$clean');
    if (await File('${created.path}/lib/main.dart').exists()) {
      await _refreshProjects();
      await _openProject(created);
    }
  }

  Future<void> _openProject(Directory directory) async {
    final mainFile = File('${directory.path}/lib/main.dart');
    if (!await mainFile.exists()) {
      _notice('This project does not have lib/main.dart.');
      return;
    }
    await _openFilePath(directory, 'lib/main.dart');
    if (!mounted) return;
    setState(() {
      _project = directory;
      _status = directory.path.split('/').last;
    });
  }

  Future<void> _openFilePath(Directory directory, String relativePath) async {
    if (!_validRelativePath(relativePath)) {
      _notice('That file path is not allowed.');
      return;
    }
    final file = File('${directory.path}/$relativePath');
    try {
      if (!await file.exists()) {
        _notice('File not found.');
        return;
      }
      if (await file.length() > 1024 * 1024) {
        _notice('This editor opens text files up to 1 MB.');
        return;
      }
      final contents = await file.readAsString();
      if (contents.contains('\u0000')) {
        _notice('This looks like a binary file.');
        return;
      }
      if (!mounted) return;
      setState(() {
        _project = directory;
        _activePath = relativePath;
        _editor.dispose();
        _editor = TextEditingController(text: contents);
        _status = 'Opened $relativePath';
      });
    } catch (error) {
      _notice('Could not open file: $error');
    }
  }

  bool _validRelativePath(String value) {
    final normalized = value.replaceAll('\\', '/');
    if (normalized.isEmpty || normalized.startsWith('/')) return false;
    final parts = normalized.split('/');
    return !parts.contains('..') && !parts.contains('.') && !normalized.contains('\u0000');
  }

  Future<void> _openFilePicker() async {
    final project = _project;
    if (project == null) {
      _notice('Create or open a project first.');
      return;
    }
    const allowed = {
      '.dart', '.yaml', '.yml', '.json', '.xml', '.kt', '.java', '.gradle', '.kts',
      '.properties', '.md', '.txt', '.html', '.css', '.js', '.svg', '.arb', '.gitignore'
    };
    final found = <String>[];
    await for (final entity in project.list(recursive: true, followLinks: false)) {
      if (entity is! File) continue;
      final path = entity.path.substring(project.path.length + 1).replaceAll('\\', '/');
      if (path.startsWith('build/') || path.startsWith('.dart_tool/') || path.startsWith('.git/')) continue;
      final fileName = path.split('/').last;
      final extension = fileName.contains('.') ? '.${fileName.split('.').last.toLowerCase()}' : fileName;
      if (allowed.contains(extension) || fileName == '.gitignore') found.add(path);
    }
    found.sort();
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: _panel,
      isScrollControlled: true,
      builder: (context) => SafeArea(
        child: SizedBox(
          height: MediaQuery.of(context).size.height * .72,
          child: Column(children: [
            const Padding(padding: EdgeInsets.all(16), child: Align(alignment: Alignment.centerLeft, child: Text('Project files', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)))),
            const Divider(height: 1, color: _line),
            Expanded(child: ListView.builder(
              itemCount: found.length,
              itemBuilder: (context, index) => ListTile(
                dense: true,
                leading: Icon(_iconFor(found[index]), size: 18, color: _blue),
                title: Text(found[index], style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
                onTap: () {
                  Navigator.pop(context);
                  _openFilePath(project, found[index]);
                },
              ),
            )),
            Padding(padding: const EdgeInsets.all(12), child: SizedBox(width: double.infinity, child: OutlinedButton.icon(onPressed: () { Navigator.pop(context); _newFile(); }, icon: const Icon(Icons.note_add), label: const Text('New file')))),
          ]),
        ),
      ),
    );
  }

  Future<void> _newFile() async {
    final project = _project;
    if (project == null) {
      _notice('Create or open a project first.');
      return;
    }
    final c = TextEditingController(text: 'lib/new_file.dart');
    final relative = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: _panel,
        title: const Text('Create source file'),
        content: TextField(controller: c, autofocus: true, decoration: const InputDecoration(labelText: 'Relative file path', helperText: 'Example: lib/screens/home.dart')),
        actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, c.text.trim()), child: const Text('Create'))],
      ),
    );
    c.dispose();
    if (relative == null || !_validRelativePath(relative)) {
      if (relative != null) _notice('Enter a valid relative path without ..');
      return;
    }
    final file = File('${project.path}/$relative');
    if (await file.exists()) {
      _notice('That file already exists.');
      return;
    }
    await file.parent.create(recursive: true);
    await file.writeAsString('');
    await _openFilePath(project, relative);
  }

  Future<void> _save() async {
    final project = _project;
    if (project == null || !_validRelativePath(_activePath)) return;
    try {
      final file = File('${project.path}/$_activePath');
      await file.parent.create(recursive: true);
      await file.writeAsString(_editor.text);
      if (!mounted) return;
      setState(() => _status = 'Saved $_activePath');
      _notice('Saved locally.');
    } catch (error) {
      _notice('Save failed: $error');
    }
  }

  Future<void> _runBuilder({required String action, required String name}) async {
    if (_busy) return;
    if (!await _ensureStorage()) return;
    final logFile = File('$_logsRoot/${action}_$name.log');
    try {
      await logFile.parent.create(recursive: true);
      await logFile.writeAsString('Preparing the embedded local Linux runtime...\n');
    } catch (_) {}
    if (!mounted) return;
    setState(() {
      _busy = true;
      _status = switch (action) {
        'setupToolchain' => 'Downloading local toolchain...',
        'createProject' => 'Creating Flutter project...',
        'buildDebug' => 'Building debug APK locally...',
        _ => 'Running local command...',
      };
      _log = 'Starting on-device build command.\n';
    });
    try {
      final result = await _builder.invokeMethod<String>(
        'runBuilder',
        {'action': action, 'projectName': name},
      );
      if (mounted && result != null) setState(() => _log = '$result\n');
      var finished = false;
      for (var attempt = 0; attempt < 1800; attempt++) {
        await Future<void>.delayed(const Duration(seconds: 2));
        if (await logFile.exists()) {
          String contents;
          try {
            contents = await logFile.readAsString();
          } catch (_) {
            continue;
          }
          if (contents.length > 24000) contents = contents.substring(contents.length - 24000);
          if (mounted) setState(() => _log = contents);
          final exitMatch = RegExp(r'LOCAL_AIDE_EXIT_CODE=(-?\d+)').firstMatch(contents);
          if (exitMatch != null) {
            finished = true;
            final success = exitMatch.group(1) == '0';
            final completedStatus = switch (action) {
              'setupToolchain' => 'Local toolchain ready',
              'createProject' => 'Project ready',
              'buildDebug' => 'Debug APK built',
              _ => 'Command finished',
            };
            if (mounted) setState(() => _status = success ? completedStatus : 'Command failed');
            if (success && action == 'buildDebug') {
              _notice('Debug APK built on this device. Check $_buildsRoot/$name-debug.apk');
            } else if (success && action == 'createProject') {
              _notice('Flutter project created locally with Android platform files.');
            } else if (success && action == 'setupToolchain') {
              _notice('Local toolchain installed. No separate terminal app is required.');
            } else if (!success) {
              _notice('The local command failed. Read the log above for details.');
            }
            break;
          }
        }
      }
      if (!finished && mounted) {
        setState(() => _status = 'Still running. The log will update while the app stays open.');
      }
    } on PlatformException catch (error) {
      if (mounted) {
        setState(() {
          _status = 'Embedded runtime unavailable';
          _log = 'Could not start the embedded Linux runtime: ${error.message ?? error.code}\n\nThe app APK must be built with the ARM64 proot-engine binaries included. Open README.md for the build steps.';
        });
      }
    } catch (error) {
      if (mounted) setState(() { _status = 'Command failed'; _log = '$error'; });
    } finally {
      if (mounted) setState(() => _busy = false);
      await _refreshProjectsSilently();
    }
  }

  Future<void> _refreshProjectsSilently() async {
    try {
      final root = Directory(_projectsRoot);
      if (!await root.exists()) return;
      final found = <Directory>[];
      await for (final entity in root.list(followLinks: false)) {
        if (entity is Directory && await File('${entity.path}/pubspec.yaml').exists() && await Directory('${entity.path}/android').exists()) found.add(entity);
      }
      found.sort((a, b) => a.path.toLowerCase().compareTo(b.path.toLowerCase()));
      if (mounted) setState(() { _projects..clear()..addAll(found); });
    } catch (_) {}
  }

  Future<void> _showSetupGuide() async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: _panel,
        title: const Text('Install the local Flutter toolchain'),
        content: const SizedBox(
          width: 480,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Local AIDE uses an embedded Android PRoot engine. When you tap Install toolchain, it downloads Ubuntu, Java 17, Flutter, Android SDK 35, and compatible ARM64 Android build tools into this app’s private storage. You do not need to install Termux or another terminal app.',
                  style: TextStyle(color: _muted, height: 1.5),
                ),
                SizedBox(height: 12),
                Text('Requirements', style: TextStyle(fontWeight: FontWeight.w700)),
                SizedBox(height: 5),
                Text('• Internet for the first setup and package downloads\n• Around 6–8 GB of free storage recommended\n• An ARM64 Android phone\n• Keep the app open while the first setup/build runs', style: TextStyle(height: 1.5)),
                SizedBox(height: 12),
                Text(
                  'Build support: the current referenced ARM64 toolchain is intended for debug APKs. Release builds may require a different host toolchain. Project code and generated APKs stay on your device; this setup does not use a remote build API.',
                  style: TextStyle(color: Color(0xFFFFD18A), height: 1.5),
                ),
                SizedBox(height: 12),
                Text(
                  'The embedded PRoot engine is open-source software. This source kit fetches it during development and includes its ARM64 native libraries in the final IDE APK. If those libraries were not included when the IDE APK was built, setup will fail and the build log will say why.',
                  style: TextStyle(color: _muted, height: 1.5, fontSize: 12),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Close')),
          FilledButton.icon(
            onPressed: _busy ? null : () {
              Navigator.pop(dialogContext);
              _runBuilder(action: 'setupToolchain', name: 'toolchain');
            },
            icon: const Icon(Icons.download_for_offline_outlined),
            label: const Text('Install toolchain'),
          ),
        ],
      ),
    );
  }

  void _notice(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message), behavior: SnackBarBehavior.floating));
  }

  IconData _iconFor(String file) {
    final ext = file.split('/').last.split('.').last.toLowerCase();
    return switch (ext) {
      'dart' || 'kt' || 'java' || 'js' => Icons.code,
      'yaml' || 'yml' || 'json' || 'xml' => Icons.data_object,
      'html' => Icons.html,
      'css' => Icons.css,
      _ => Icons.description_outlined,
    };
  }

  @override
  void dispose() {
    _editor.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          titleSpacing: 12,
          title: const Row(children: [
            Icon(Icons.code_rounded, color: _blue, size: 29),
            SizedBox(width: 9),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text('LOCAL AIDE', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, letterSpacing: 1.0)),
              Text('On-device Flutter studio', style: TextStyle(fontSize: 10, color: _muted)),
            ])),
          ]),
          actions: [
            IconButton(tooltip: 'Setup guide', onPressed: _showSetupGuide, icon: const Icon(Icons.help_outline)),
            IconButton(tooltip: 'Refresh projects', onPressed: _refreshProjects, icon: const Icon(Icons.refresh)),
            IconButton(tooltip: 'New project', onPressed: _busy ? null : _createProject, icon: const Icon(Icons.create_new_folder_outlined)),
          ],
        ),
        body: Column(children: [
          Container(height: 1, color: _line),
          Container(
            color: _panel,
            height: 42,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(children: [
              const Icon(Icons.folder_open, size: 17, color: _blue),
              const SizedBox(width: 8),
              Expanded(child: Text(_projectName.isEmpty ? 'No project selected' : _projectName, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12), overflow: TextOverflow.ellipsis)),
              TextButton.icon(onPressed: _project == null ? null : _openFilePicker, icon: const Icon(Icons.folder, size: 15), label: const Text('Files', style: TextStyle(fontSize: 11))),
              TextButton.icon(onPressed: _project == null ? null : _newFile, icon: const Icon(Icons.note_add_outlined, size: 15), label: const Text('New file', style: TextStyle(fontSize: 11))),
            ]),
          ),
          Expanded(child: Row(children: [
            Container(
              width: 148,
              decoration: const BoxDecoration(color: _panel, border: Border(right: BorderSide(color: _line))),
              child: Column(children: [
                const Padding(padding: EdgeInsets.fromLTRB(10, 12, 8, 8), child: Align(alignment: Alignment.centerLeft, child: Text('PROJECTS', style: TextStyle(color: _muted, fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: .8)))),
                Expanded(child: _projects.isEmpty
                    ? const Padding(padding: EdgeInsets.all(10), child: Text('No projects yet. Tap + to create one.', style: TextStyle(color: _muted, fontSize: 11, height: 1.5)))
                    : ListView.builder(
                        itemCount: _projects.length,
                        itemBuilder: (context, index) {
                          final p = _projects[index];
                          final chosen = p.path == _project?.path;
                          return ListTile(
                            dense: true,
                            selected: chosen,
                            selectedTileColor: _panel2,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 8),
                            leading: const Icon(Icons.folder, size: 16, color: _blue),
                            title: Text(p.path.split('/').last, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11)),
                            onTap: () => _openProject(p),
                          );
                        },
                      )),
                Padding(padding: const EdgeInsets.all(6), child: SizedBox(width: double.infinity, child: OutlinedButton.icon(onPressed: _busy ? null : _createProject, icon: const Icon(Icons.add, size: 15), label: const Text('New project', style: TextStyle(fontSize: 10))))),
              ]),
            ),
            Expanded(child: Column(children: [
              Container(height: 34, color: const Color(0xFF101925), padding: const EdgeInsets.symmetric(horizontal: 10), child: Row(children: [
                Icon(_iconFor(_activePath), size: 15, color: _blue),
                const SizedBox(width: 7),
                Expanded(child: Text(_activePath, style: const TextStyle(fontSize: 10, color: _muted), overflow: TextOverflow.ellipsis)),
                const Text('UTF-8', style: TextStyle(fontSize: 9, color: _muted)),
                IconButton(tooltip: 'Save file', visualDensity: VisualDensity.compact, onPressed: _save, icon: const Icon(Icons.save_outlined, size: 17)),
              ])),
              Expanded(child: Container(
                color: const Color(0xFF0D131D),
                padding: const EdgeInsets.fromLTRB(12, 8, 8, 8),
                child: TextField(
                  controller: _editor,
                  expands: true,
                  maxLines: null,
                  minLines: null,
                  keyboardType: TextInputType.multiline,
                  textAlignVertical: TextAlignVertical.top,
                  autocorrect: false,
                  enableSuggestions: false,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12, height: 1.6, color: _text),
                  cursorColor: _blue,
                  decoration: const InputDecoration(border: InputBorder.none, hintText: 'Create a project to start editing.'),
                ),
              )),
              Container(height: 40, color: _panel, padding: const EdgeInsets.symmetric(horizontal: 9), child: Row(children: [
                const Icon(Icons.circle, size: 7, color: Color(0xFF64D9A2)),
                const SizedBox(width: 6),
                Expanded(child: Text(_status, style: const TextStyle(fontSize: 10, color: _muted), maxLines: 1, overflow: TextOverflow.ellipsis)),
                FilledButton.icon(
                  onPressed: _busy || _project == null ? null : () async { await _save(); await _runBuilder(action: 'buildDebug', name: _projectName); },
                  icon: _busy ? const SizedBox(width: 13, height: 13, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.build_circle_outlined, size: 16),
                  label: Text(_busy ? 'Working' : 'Build Debug APK'),
                  style: FilledButton.styleFrom(textStyle: const TextStyle(fontSize: 10), padding: const EdgeInsets.symmetric(horizontal: 9)),
                ),
              ])),
              Container(
                height: 126,
                width: double.infinity,
                color: const Color(0xFF080D14),
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 4),
                child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                  const Row(children: [Icon(Icons.terminal, size: 14, color: _blue), SizedBox(width: 6), Text('BUILD OUTPUT', style: TextStyle(fontSize: 9, color: _muted, fontWeight: FontWeight.w800, letterSpacing: .8))]),
                  const SizedBox(height: 5),
                  Expanded(child: SingleChildScrollView(child: SelectableText(_log, style: const TextStyle(fontFamily: 'monospace', fontSize: 9, height: 1.35, color: Color(0xFFB5C7D9))))),
                ]),
              ),
            ])),
          ])),
          Container(height: 24, color: _panel, padding: const EdgeInsets.symmetric(horizontal: 10), child: Row(children: [
            Icon(Icons.circle, size: 7, color: _storageReady ? const Color(0xFF64D9A2) : const Color(0xFFFFC46B)),
            const SizedBox(width: 6),
            Expanded(child: Text(_storageReady ? 'LOCAL WORKSPACE  ·  $_projectsRoot' : 'TOOLCHAIN SETUP  ·  Tap help to install tools', style: const TextStyle(fontSize: 8, color: _muted), overflow: TextOverflow.ellipsis)),
            const Text('DEBUG APK', style: TextStyle(fontSize: 8, color: _blue, fontWeight: FontWeight.w800)),
          ])),
        ]),
      );
}
