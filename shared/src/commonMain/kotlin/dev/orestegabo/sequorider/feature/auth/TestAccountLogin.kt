package dev.orestegabo.sequorider.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TestAccountLoginEntry(available: suspend () -> Boolean, onSignIn: suspend (String,String) -> Unit,
    french: Boolean = false, modifier: Modifier = Modifier) {
    var enabled by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit) { try { enabled=available() } catch(e:CancellationException){throw e} catch(_:Exception){} }
    if (!enabled) return
    TextButton(onClick={open=true;failed=false},modifier=modifier) { Text(if(french) "Connexion au compte de test" else "Sign in to a test account") }
    if(open) AlertDialog(onDismissRequest={if(!busy){open=false;password=""}},
        title={Text(if(french) "Compte de test" else "Test account")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(french) "Utilisez les identifiants prives du guide de test." else "Use the private credentials from your testing guide.",style=MaterialTheme.typography.bodySmall)
            OutlinedTextField(username,{username=it;failed=false},label={Text(if(french) "Nom du compte" else "Username")},singleLine=true,enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii,autoCorrectEnabled=false))
            OutlinedTextField(password,{password=it;failed=false},label={Text(if(french) "Mot de passe" else "Password")},singleLine=true,enabled=!busy,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrectEnabled=false))
            if(failed) Text(if(french) "Connexion refusee. Verifiez les identifiants, l'application et la date d'expiration du compte." else "Sign-in failed. Check the credentials, app and account expiry.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }},
        confirmButton={TextButton(enabled=!busy && username.isNotBlank() && password.isNotBlank(),onClick={
            busy=true;failed=false
            scope.launch {try {onSignIn(username.trim(),password);password="";open=false}
                catch(e:CancellationException){throw e} catch(_:Exception){failed=true}
                finally{busy=false}}
        }){Text(if(busy) (if(french) "Connexion…" else "Signing in…") else (if(french) "Se connecter" else "Sign in"))}},
        dismissButton={TextButton(enabled=!busy,onClick={open=false;password=""}){Text(if(french) "Annuler" else "Cancel")}})
}
