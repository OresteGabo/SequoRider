import UIKit
import SwiftUI
import Shared
import GoogleSignIn

private let googleIosClientId = "92804430098-76fv9fnb868enoqhpf9v954rvuogfhqk.apps.googleusercontent.com"

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            onGoogleSignIn: { complete in
                GoogleSignInPresenter.signIn { result in
                    _ = complete(result)
                }
            },
            onGoogleSignOut: {
                GoogleSignInPresenter.signOut()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

private enum GoogleSignInPresenter {
    static func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    static func signIn(complete: @escaping (any GoogleSignInResult) -> Void) {
        guard let presenter = topViewController() else {
            complete(GoogleSignInResultFailure(reason: GoogleSignInFailureReason.unavailable))
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: googleIosClientId)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                if error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue {
                    complete(GoogleSignInResultCancelled.shared)
                } else {
                    complete(GoogleSignInResultFailure(reason: GoogleSignInFailureReason.failed))
                }
                return
            }

            guard let user = result?.user, let idToken = user.idToken?.tokenString, !idToken.isEmpty else {
                complete(GoogleSignInResultFailure(reason: GoogleSignInFailureReason.unavailable))
                return
            }

            complete(
                GoogleSignInResultSuccess(
                    idToken: idToken,
                    email: user.profile?.email,
                    displayName: user.profile?.name
                )
            )
        }
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let root = scenes
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
        return topViewController(from: root)
    }

    private static func topViewController(from root: UIViewController?) -> UIViewController? {
        if let navigation = root as? UINavigationController {
            return topViewController(from: navigation.visibleViewController)
        }
        if let tab = root as? UITabBarController {
            return topViewController(from: tab.selectedViewController)
        }
        if let presented = root?.presentedViewController {
            return topViewController(from: presented)
        }
        return root
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
